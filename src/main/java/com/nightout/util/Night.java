package com.nightout.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Night {

    public static final int ENDS_AT_HOUR=6;

    private Night() {}

    /** The date the current night started on */
    public static LocalDate current() {
        LocalDateTime now = LocalDateTime.now();
        return now.getHour() < ENDS_AT_HOUR ? now.toLocalDate().minusDays(1) : now.toLocalDate();
    }
}
