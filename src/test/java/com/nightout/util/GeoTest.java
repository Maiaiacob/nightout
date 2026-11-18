package com.nightout.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoTest {

    @Test
    void samePointIsZero() {
        assertThat(Geo.distanceMeters(52.0116, 4.3571, 52.0116, 4.3571)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111Km() {
        assertThat(Geo.distanceMeters(0, 0, 1, 0)).isCloseTo(111_195, within(50.0));
    }

    @Test
    void delftToRotterdamCentraal() {
        // Delft Markt -> Rotterdam Centraal, roughly 12.5 km in a straight line
        assertThat(Geo.distanceMeters(52.0116, 4.3571, 51.9244, 4.4690)).isCloseTo(12_500, within(500.0));
    }
}
