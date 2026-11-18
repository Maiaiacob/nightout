package com.nightout.repository;

import com.nightout.TestcontainersConfiguration;
import com.nightout.model.Address;
import com.nightout.model.City;
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
@Transactional   // each test is rolled back
class VenueRepoNearbyTest {

    // Delft, Markt
    private static final double LAT = 52.0116;
    private static final double LNG = 4.3571;

    @Autowired CityRepo cityRepo;
    @Autowired VenueRepo venueRepo;

    private City delft;

    @BeforeEach
    void setUp() {
        delft = cityRepo.saveAndFlush(new City("Delft", "Netherlands"));
    }

    private Venue venue(String name, Double lat, Double lng) {
        return venueRepo.saveAndFlush(new Venue(name, null,
                new Address("Street", "1", "2611", lat, lng), VenueType.BAR, delft));
    }

    @Test
    void returnsOnlyVenuesInsideRadius() {
        venue("Close", 52.0125, 4.3580);      // ~120 m away
        venue("Far", 52.0400, 4.3200);        // ~4 km away
        venue("No coordinates", null, null);

        List<Venue> result = venueRepo.findNearby(LAT, LNG, 1000, 50);

        assertThat(result).extracting(Venue::getName).containsExactly("Close");
    }

    @Test
    void ordersNearestFirst() {
        venue("700 m", 52.0179, 4.3571);
        venue("100 m", 52.0125, 4.3571);
        venue("400 m", 52.0152, 4.3571);

        List<Venue> result = venueRepo.findNearby(LAT, LNG, 1000, 50);

        assertThat(result).extracting(Venue::getName).containsExactly("100 m", "400 m", "700 m");
    }

    @Test
    void respectsLimit() {
        for (int i = 0; i < 5; i++) venue("Bar " + i, 52.0120 + i * 0.0001, 4.3571);

        assertThat(venueRepo.findNearby(LAT, LNG, 1000, 3)).hasSize(3);
    }
}
