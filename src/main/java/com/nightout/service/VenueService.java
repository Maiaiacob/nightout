package com.nightout.service;

import com.nightout.dto.NearbyVenueDto;
import com.nightout.dto.VenueDto;
import com.nightout.hours.OpeningHoursService;
import com.nightout.hours.WeekTime;
import com.nightout.mapper.VenueMapper;
import com.nightout.model.Venue;
import com.nightout.model.VenueType;
import com.nightout.repository.VenueRepo;
import com.nightout.util.Geo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VenueService {
    private final VenueRepo venueRepository;
    private final VenueMapper venueMapper;
    private final Clock clock;
    private final OpeningHoursService openingHoursService;

    public VenueService(VenueRepo venueRepository, VenueMapper venueMapper, Clock clock,
                        OpeningHoursService openingHoursService) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
        this.clock = clock;
        this.openingHoursService = openingHoursService;
    }

    public List<VenueDto> findByCity(String city, VenueType type) {
        List<Venue> venues= (type == null)
                ? venueRepository.findByCityNameIgnoreCase(city)
                : venueRepository.findByCityNameIgnoreCaseAndType(city, type);

        return venues.stream()
                .map(venueMapper::toDto)
                .toList();
    }

    public static final int MAX_RADIUS_METERS=10_000;
    private static final int MAX_RESULTS=200;

    /** openNow=only venues whose opening hours say they are open right now. */
    public List<NearbyVenueDto> findNearby(double lat, double lng, int radiusMeters, boolean openNow) {
        if (lat<-90 || lat>90 || lng<-180 || lng>180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates");
        }
        if (radiusMeters<=0 || radiusMeters>MAX_RADIUS_METERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "radius must be between 1 and " + MAX_RADIUS_METERS + " metres");
        }

        List<Venue> venues;
        if (openNow) {
            WeekTime now=WeekTime.of(LocalDateTime.now(clock));
            venues=venueRepository.findNearbyOpenAt(lat, lng, radiusMeters, MAX_RESULTS,
                    now.day(), now.previousDay(), now.minute());
        } else {
            venues=venueRepository.findNearby(lat, lng, radiusMeters, MAX_RESULTS);
        }
        var openStatus=openingHoursService.openNow(venues);
        return venues.stream()
                .map(v -> new NearbyVenueDto(venueMapper.toDto(v, openStatus.get(v.getId())), (int) Math.round(Geo.distanceMeters(
                        lat, lng, v.getAddress().getLatitude(), v.getAddress().getLongitude()))))
                .toList();
    }

    public VenueDto findById(Long id) {
        Venue venue=venueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venue " + id + " not found"));
        return venueMapper.toDto(venue);
    }
}
