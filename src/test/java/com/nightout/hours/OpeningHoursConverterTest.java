package com.nightout.hours;

import com.nightout.hours.OpeningHoursConverter.Period;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpeningHoursConverterTest {

    private final OpeningHoursConverter converter = new OpeningHoursConverter();

    private List<Period> convert(String hours) {
        return converter.convert(hours).orElseThrow(() -> new AssertionError("Could not convert: " + hours));
    }

    private static int t(int hour, int minute) { return hour * 60 + minute; }

    @Test
    void closingAfterMidnightStaysOnTheDayItOpened() {
        assertThat(convert("Mo-Th 16:00-01:00; Fr-Sa 16:00-03:00")).containsExactly(
                new Period(1, t(16, 0), t(25, 0)),
                new Period(2, t(16, 0), t(25, 0)),
                new Period(3, t(16, 0), t(25, 0)),
                new Period(4, t(16, 0), t(25, 0)),
                new Period(5, t(16, 0), t(27, 0)),
                new Period(6, t(16, 0), t(27, 0)));
    }

    @Test
    void twentyFourSeven() {
        assertThat(convert("24/7")).hasSize(7).allMatch(p -> p.opensMinute() == 0 && p.closesMinute() == 1440);
    }

    @Test
    void laterRuleReplacesEarlierOneForItsDays() {
        List<Period> periods = convert("Mo-Su 12:00-23:00; Su 14:00-22:00");
        assertThat(periods).hasSize(7);
        assertThat(periods).filteredOn(p -> p.dayOfWeek() == 7).containsExactly(new Period(7, t(14, 0), t(22, 0)));
        assertThat(periods).filteredOn(p -> p.dayOfWeek() == 1).containsExactly(new Period(1, t(12, 0), t(23, 0)));
    }

    @Test
    void offRemovesTheDay() {
        List<Period> periods = convert("Mo-Sa 17:00-24:00; Su off");
        assertThat(periods).hasSize(6).noneMatch(p -> p.dayOfWeek() == 7);
        assertThat(periods.get(0)).isEqualTo(new Period(1, t(17, 0), t(24, 0)));
    }

    @Test
    void twoTimeSpansOnOneDay() {
        assertThat(convert("Mo 10:00-12:00,14:00-18:00")).containsExactly(
                new Period(1, t(10, 0), t(12, 0)),
                new Period(1, t(14, 0), t(18, 0)));
    }

    @Test
    void dayRangeWrapsAroundTheWeekend() {
        assertThat(convert("Fr-Mo 20:00-04:00")).extracting(Period::dayOfWeek).containsExactly(1, 5, 6, 7);
    }

    @Test
    void publicHolidayRuleIsIgnoredButWeeklyHoursStay() {
        assertThat(convert("Mo-Fr 17:00-01:00; PH off")).hasSize(5);
    }

    @Test
    void openEndIsAssumedToCloseAt3() {
        assertThat(convert("Fr 18:00+")).containsExactly(new Period(5, t(18, 0), t(27, 0)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ask the bartender", "sunset-02:00", " "})
    void unknownOrUnsupportedGivesEmpty(String hours) {
        assertThat(converter.convert(hours)).isEmpty();
    }
}
