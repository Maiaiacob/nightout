package com.nightout.hours;

import com.nightout.model.OpeningPeriod;
import com.nightout.model.Venue;
import com.nightout.repository.OpeningPeriodRepo;
import com.nightout.repository.VenueRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OpeningHoursService {

    public record BackfillResult(int venues, int withHoursText, int parsed) {}

    /** open = null means the opening hours are unknown; until = closing time like "03:00" when open */
    public record OpenNow(Boolean open, String until) {}

    private final OpeningHoursConverter converter;
    private final OpeningPeriodRepo periodRepo;
    private final VenueRepo venueRepo;
    private final Clock clock;

    public OpeningHoursService(OpeningHoursConverter converter, OpeningPeriodRepo periodRepo,
                               VenueRepo venueRepo, Clock clock) {
        this.converter = converter;
        this.periodRepo = periodRepo;
        this.venueRepo = venueRepo;
        this.clock = clock;
    }

    /** Which of these venues are open right now, and until when (one query for the whole list) */
    @Transactional(readOnly = true)
    public Map<Long, OpenNow> openNow(Collection<Venue> venues) {
        List<Long> known = venues.stream()
                .filter(v -> Boolean.TRUE.equals(v.getHoursParsed()))
                .map(Venue::getId)
                .toList();
        Map<Long, OpenNow> result = new HashMap<>();
        if (known.isEmpty()) return result;

        WeekTime now = WeekTime.of(LocalDateTime.now(clock));
        Map<Long, Integer> closes = new HashMap<>();
        for (Object[] row : periodRepo.findOpenSlots(known, now.day(), now.previousDay(), now.minute())) {
            closes.merge(((Number) row[0]).longValue(), ((Number) row[1]).intValue(), Math::max);
        }
        for (Long id : known) {
            Integer close = closes.get(id);
            result.put(id, close == null ? new OpenNow(false, null) : new OpenNow(true, hhmm(close)));
        }
        return result;
    }

    private static String hhmm(int minutes) {
        int m = minutes % (24 * 60);   // 1620 (Friday's 03:00 on Saturday) -> 180 -> "03:00"
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    /** (Re)build the time slots of one venue from its opening_hours text */
    @Transactional
    public void apply(Venue venue) {
        if (venue.getHoursParsed() != null) periodRepo.deleteByVenue(venue);   // new venues have nothing to delete

        Optional<List<OpeningHoursConverter.Period>> periods = converter.convert(venue.getOpeningHours());
        venue.setHoursParsed(periods.isPresent());
        periods.ifPresent(list -> list.forEach(p ->
                periodRepo.save(new OpeningPeriod(venue, p.dayOfWeek(), p.opensMinute(), p.closesMinute()))));
    }

    /** Convert all venues that were imported before this feature existed */
    @Transactional
    public BackfillResult backfill() {
        List<Venue> venues = venueRepo.findByHoursParsedIsNull();
        int withText = 0, parsed = 0;
        for (Venue venue : venues) {
            apply(venue);
            if (venue.getOpeningHours() != null) withText++;
            if (Boolean.TRUE.equals(venue.getHoursParsed())) parsed++;
        }
        return new BackfillResult(venues.size(), withText, parsed);
    }
}
