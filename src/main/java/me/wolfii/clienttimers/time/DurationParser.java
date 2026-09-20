package me.wolfii.clienttimers.time;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern TOKEN = Pattern.compile("(\\d+)([a-zA-Z]+)");
    private static final long MILLIS_SECOND = 1000L;
    private static final long MILLIS_MINUTE = 60L * MILLIS_SECOND;
    private static final long MILLIS_HOUR = 60L * MILLIS_MINUTE;
    private static final long MILLIS_DAY = 24L * MILLIS_HOUR;
    private static final long MILLIS_WEEK = 7L * MILLIS_DAY;
    private static final long MILLIS_MONTH = 30L * MILLIS_DAY;
    private static final long MILLIS_YEAR = 365L * MILLIS_DAY;

    private DurationParser() {
    }

    public static ParsedDuration parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("empty duration");
        }
        String input = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        Matcher matcher = TOKEN.matcher(input);
        int consumed = 0;
        long ticks = 0;
        long millis = 0;
        boolean any = false;
        while (matcher.find()) {
            if (matcher.start() != consumed) {
                throw new IllegalArgumentException("invalid duration: " + raw);
            }
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "t", "tick", "ticks" -> ticks += amount;
                case "s", "sec", "secs", "second", "seconds" -> millis += amount * MILLIS_SECOND;
                case "m", "min", "mins", "minute", "minutes" -> millis += amount * MILLIS_MINUTE;
                case "h", "hr", "hrs", "hour", "hours" -> millis += amount * MILLIS_HOUR;
                case "d", "day", "days" -> millis += amount * MILLIS_DAY;
                case "w", "wk", "wks", "week", "weeks" -> millis += amount * MILLIS_WEEK;
                case "mo", "month", "months" -> millis += amount * MILLIS_MONTH;
                case "y", "yr", "yrs", "year", "years" -> millis += amount * MILLIS_YEAR;
                default -> throw new IllegalArgumentException("unknown duration unit: " + unit);
            }
            consumed = matcher.end();
            any = true;
        }
        if (!any || consumed != input.length()) {
            throw new IllegalArgumentException("invalid duration: " + raw);
        }
        return new ParsedDuration(ticks, millis, raw.trim());
    }

    public static String format(ParsedDuration duration, ClockMode mode) {
        if (mode == ClockMode.TICKS_PLAYING) {
            return formatPlayingTicks(duration.ticksFor(mode));
        }
        return formatMillis(duration.millis());
    }

    public static String formatTicks(long ticks) {
        if (ticks < 0) ticks = 0;
        return formatMillis(ticks * 50L);
    }

    public static String formatPlayingTicks(long ticks) {
        if (ticks < 0) ticks = 0;
        return formatTicks(ticks) + " (" + ticks + "t)";
    }

    public static String formatMillis(long millis) {
        if (millis < 0) millis = 0;
        long remaining = millis;
        StringBuilder builder = new StringBuilder();
        long years = remaining / MILLIS_YEAR;
        remaining %= MILLIS_YEAR;
        long months = remaining / MILLIS_MONTH;
        remaining %= MILLIS_MONTH;
        long weeks = remaining / MILLIS_WEEK;
        remaining %= MILLIS_WEEK;
        long days = remaining / MILLIS_DAY;
        remaining %= MILLIS_DAY;
        long hours = remaining / MILLIS_HOUR;
        remaining %= MILLIS_HOUR;
        long minutes = remaining / MILLIS_MINUTE;
        remaining %= MILLIS_MINUTE;
        long seconds = remaining / MILLIS_SECOND;
        remaining %= MILLIS_SECOND;
        append(builder, years, "y");
        append(builder, months, "mo");
        append(builder, weeks, "w");
        append(builder, days, "d");
        append(builder, hours, "h");
        append(builder, minutes, "min");
        append(builder, seconds, "s");
        append(builder, remaining, "ms");
        if (builder.isEmpty()) {
            return "0ms";
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, long amount, String unit) {
        if (amount <= 0) {
            return;
        }
        builder.append(amount).append(unit);
    }
}
