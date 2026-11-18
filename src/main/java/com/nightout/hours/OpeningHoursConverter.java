package com.nightout.hours;

import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.WeekDayRange;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Turns an OpenStreetMap opening_hours string into weekly time slots.
 *
 * The parsing itself is done by the OpeningHoursParser library. This class only supports the weekly part
 * of the spec, which covers almost all bars:
 *   - day ranges ("Mo-Th", "Fr-Mo"), times ("16:00-01:00"), several times per day, "24/7", "off"
 *   - later rules replace earlier ones for the days they mention ("Mo-Su 12:00-23:00; Su 14:00-22:00")
 *   - rules for specific dates or public holidays ("PH off", "Dec 25 off") are ignored: the weekly hours still apply
 * Anything we can't represent safely (sunset, "unknown", "every 1st Monday", free text) gives Optional.empty(),
 * meaning "hours unknown", rather than a guess.
 */
@Component
public class OpeningHoursConverter {

    /** dayOfWeek 1 = Monday ... 7 = Sunday; closesMinute can be past 1440 (closes after midnight). */
    public record Period(int dayOfWeek, int opensMinute, int closesMinute) {}

    static final int DAY = 24 * 60;
    /** "18:00+" (open end): we assume it closes at 03:00 the next morning */
    static final int OPEN_END_CLOSES = DAY + 3 * 60;

    private static final List<String> DAY_NAMES = List.of("MO", "TU", "WE", "TH", "FR", "SA", "SU");

    public Optional<List<Period>> convert(String openingHours) {
        if (openingHours == null || openingHours.isBlank()) return Optional.empty();

        List<Rule> rules;
        try {
            rules = new OpeningHoursParser(new ByteArrayInputStream(openingHours.getBytes(StandardCharsets.UTF_8)))
                    .rules(false);   // non-strict: accept the small mistakes that are common in OSM data
        } catch (Exception e) {
            return Optional.empty();
        } catch (Error e) {            // the generated parser throws an Error for some invalid characters
            if (e instanceof VirtualMachineError) throw e;
            return Optional.empty();
        }

        Map<Integer, List<int[]>> week = new TreeMap<>();
        for (Rule rule : rules) {
            if (rule.isFallBack()) continue;                  // "||" fallback rules: rare, skip
            if (hasAny(rule.getYears()) || hasAny(rule.getWeeks())
                    || hasAny(rule.getDates()) || hasAny(rule.getHolidays())) continue;   // special dates: skip

            String modifier = rule.getModifier() == null ? "" : String.valueOf(rule.getModifier().getModifier()).toUpperCase();
            if (modifier.equals("UNKNOWN")) return Optional.empty();
            boolean closed = modifier.equals("CLOSED") || modifier.equals("OFF");

            List<Integer> days = days(rule);
            if (days == null) return Optional.empty();

            boolean hasTimes = hasAny(rule.getTimes());
            if (!closed && !hasTimes && !rule.isTwentyfourseven() && rule.getDays() == null) {
                return Optional.empty();                      // e.g. only a comment: "call for hours"
            }

            List<int[]> spans = new ArrayList<>();
            if (closed) {
                if (hasTimes) return Optional.empty();        // "Mo 12:00-14:00 off" (closed for part of a day): not supported
            } else if (rule.isTwentyfourseven() || !hasTimes) {
                spans.add(new int[]{0, DAY});                 // "24/7" or "Sa" with no times = open all day
            } else {
                for (TimeSpan t : rule.getTimes()) {
                    int[] span = span(t);
                    if (span == null) return Optional.empty();
                    spans.add(span);
                }
            }

            for (int day : days) {
                // A normal rule replaces what earlier rules said about this day; an additional rule (",") adds to it
                List<int[]> daySpans = rule.isAdditive() ? week.computeIfAbsent(day, d -> new ArrayList<>()) : new ArrayList<>();
                daySpans.addAll(spans);
                week.put(day, daySpans);
            }
        }

        List<Period> periods = new ArrayList<>();
        week.forEach((day, spans) -> spans.stream()
                .sorted(Comparator.comparingInt(s -> s[0]))
                .forEach(s -> periods.add(new Period(day, s[0], s[1]))));
        return Optional.of(periods);
    }

    /** The days a rule applies to (1-7), or null if it uses something we don't support ("Mo[1]") */
    private List<Integer> days(Rule rule) {
        if (!hasAny(rule.getDays())) return List.of(1, 2, 3, 4, 5, 6, 7);
        List<Integer> result = new ArrayList<>();
        for (WeekDayRange range : rule.getDays()) {
            if (hasAny(range.getNths())) return null;
            int start = dayNumber(range.getStartDay());
            int end = range.getEndDay() == null ? start : dayNumber(range.getEndDay());
            if (start < 0 || end < 0) return null;
            // Wraps around the weekend: "Fr-Mo" = Fr, Sa, Su, Mo
            for (int d = start; ; d = d % 7 + 1) {
                if (!result.contains(d)) result.add(d);
                if (d == end) break;
            }
        }
        return result;
    }

    private static int dayNumber(Object day) {
        String name = String.valueOf(day).toUpperCase();   // "MO", "TU", ...
        int index = name.length() < 2 ? -1 : DAY_NAMES.indexOf(name.substring(0, 2));
        return index < 0 ? -1 : index + 1;
    }

    /** [opens, closes] in minutes, with closes past 1440 when it closes after midnight; null if not supported */
    private static int[] span(TimeSpan t) {
        if (t.getStartEvent() != null || t.getEndEvent() != null) return null;   // sunrise, sunset, ...
        int start = t.getStart();
        if (start == TimeSpan.UNDEFINED_TIME) return null;

        int end = t.getEnd();
        if (end == TimeSpan.UNDEFINED_TIME) {
            if (!t.isOpenEnded()) return null;                // a single point in time, not a range
            end = OPEN_END_CLOSES;
        }
        if (end <= start) end += DAY;                         // "22:00-03:00" -> closes 03:00 the next day
        end = Math.min(end, 2 * DAY);
        return new int[]{start, end};
    }

    private static boolean hasAny(Collection<?> c) {
        return c != null && !c.isEmpty();
    }
}
