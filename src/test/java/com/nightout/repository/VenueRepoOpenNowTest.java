package com.nightout.repository;

import com.nightout.TestcontainersConfiguration;
import com.nightout.model.Address;
import com.nightout.model.City;
import com.nightout.model.OpeningPeriod;
import com.nightout.model.Venue;
import com.nightout.model.VenueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "nightout.preload.enabled=false")
@Import(TestcontainersConfiguration.class)
@Transactional
class VenueRepoOpenNowTest {

    private static final double LAT = 52.0116, LNG = 4.3571;   // Delft
    private static final int FRIDAY = 5, SATURDAY = 6;

    @Autowired CityRepo cityRepo;
    @Autowired VenueRepo venueRepo;
    @Autowired OpeningPeriodRepo periodRepo;

    @BeforeEach
    void setUp() {
        City delft = cityRepo.saveAndFlush(new City("Delft", "Netherlands"));

        // Fri 20:00 - Sat 03:00
        Venue lateBar = venueRepo.save(new Venue("Late bar", null, new Address("A", "1", "2611", 52.0120, 4.3571), VenueType.BAR, delft));
        periodRepo.save(new OpeningPeriod(lateBar, FRIDAY, 20 * 60, 27 * 60));

        // Sat 20:00 - 24:00
        Venue saturdayBar = venueRepo.save(new Venue("Saturday bar", null, new Address("B", "1", "2611", 52.0121, 4.3571), VenueType.BAR, delft));
        periodRepo.save(new OpeningPeriod(saturdayBar, SATURDAY, 20 * 60, 24 * 60));

        // No opening hours known
        venueRepo.save(new Venue("Unknown bar", null, new Address("C", "1", "2611", 52.0122, 4.3571), VenueType.BAR, delft));
        venueRepo.flush();
        periodRepo.flush();
    }

    private List<String> openAt(int day, int hour, int minute) {
        int previousDay = day == 1 ? 7 : day - 1;
        return venueRepo.findNearbyOpenAt(LAT, LNG, 1000, 50, day, previousDay, hour * 60 + minute)
                .stream().map(Venue::getName).toList();
    }

    @Test
    void fridayEvening() {
        assertThat(openAt(FRIDAY, 21, 0)).containsExactly("Late bar");
    }

    @Test
    void saturdayAfterMidnightTheFridayBarIsStillOpen() {
        assertThat(openAt(SATURDAY, 1, 0)).containsExactly("Late bar");
    }

    @Test
    void saturdayAtClosingTimeItIsClosed() {
        assertThat(openAt(SATURDAY, 3, 0)).isEmpty();
    }

    @Test
    void saturdayNight() {
        assertThat(openAt(SATURDAY, 22, 0)).containsExactly("Saturday bar");
    }

    @Test
    void openSlotsGiveTheClosingTimeOfTheSlotThatIsRunning() {
        List<Long> ids = venueRepo.findAll().stream().map(Venue::getId).toList();
        List<Object[]> rows = periodRepo.findOpenSlots(ids, SATURDAY, FRIDAY, 60);   // Saturday 01:00

        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0)[1]).intValue()).isEqualTo(27 * 60);       // Friday's slot, closes 03:00
    }

    @Test
    void unknownHoursAreNeverOpen() {
        assertThat(openAt(SATURDAY, 22, 0)).doesNotContain("Unknown bar");
    }
}
