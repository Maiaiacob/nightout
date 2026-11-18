package com.nightout.hours;

import java.time.LocalDateTime;

/**
 * A moment in the week, in the form the opening-hours query needs.
 * previousDay is there for bars that opened yesterday and are still open after midnight.
 */
public record WeekTime(int day, int previousDay, int minute) {

    public static WeekTime of(LocalDateTime time) {
        int day = time.getDayOfWeek().getValue();             // 1 = Monday ... 7 = Sunday
        int previousDay = day == 1 ? 7 : day - 1;
        return new WeekTime(day, previousDay, time.getHour() * 60 + time.getMinute());
    }
}
