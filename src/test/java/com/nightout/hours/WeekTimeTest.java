package com.nightout.hours;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WeekTimeTest {

    @Test
    void saturdayAtOneAm() {
        // 2026-11-21 is a Saturday
        assertThat(WeekTime.of(LocalDateTime.of(2026, 11, 21, 1, 30))).isEqualTo(new WeekTime(6, 5, 90));
    }

    @Test
    void mondayLooksBackToSunday() {
        assertThat(WeekTime.of(LocalDateTime.of(2026, 11, 23, 0, 15))).isEqualTo(new WeekTime(1, 7, 15));
    }
}
