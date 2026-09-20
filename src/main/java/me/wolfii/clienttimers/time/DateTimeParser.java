package me.wolfii.clienttimers.time;

import me.wolfii.clienttimers.timer.AlarmTarget;

import java.time.*;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateTimeParser {
    private static final Pattern GAME = Pattern.compile("^(\\d+)([td])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISO_DATE = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})(?:[tT_\\- ](.+))?$");
    private static final Pattern SLASH_DATE = Pattern.compile("^(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?(?:[tT_\\- ](.+))?$");
    private static final Pattern TIME = Pattern.compile("^(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)?$", Pattern.CASE_INSENSITIVE);

    private DateTimeParser() {
    }

    public static AlarmTarget parse(String raw, DateOrder dateOrder, ZonedDateTime now) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("empty time");
        }
        String input = raw.trim();
        Matcher game = GAME.matcher(input.replace(" ", ""));
        if (game.matches()) {
            long value = Long.parseLong(game.group(1));
            if (game.group(2).equalsIgnoreCase("t")) {
                return new AlarmTarget.GameTime(value);
            }
            return new AlarmTarget.GameDay(value);
        }
        Matcher iso = ISO_DATE.matcher(input);
        if (iso.matches()) {
            LocalDate date = LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3)));
            LocalTime time = iso.group(4) == null ? LocalTime.MIDNIGHT : parseTime(iso.group(4));
            return new AlarmTarget.WallTime(ZonedDateTime.of(date, time, now.getZone()));
        }
        Matcher slash = SLASH_DATE.matcher(input);
        if (slash.matches()) {
            int first = Integer.parseInt(slash.group(1));
            int second = Integer.parseInt(slash.group(2));
            Integer year = slash.group(3) == null ? null : normalizeYear(Integer.parseInt(slash.group(3)), now.getYear());
            LocalDate date = resolveSlashDate(first, second, year, dateOrder, now.toLocalDate());
            LocalTime time = slash.group(4) == null ? LocalTime.MIDNIGHT : parseTime(slash.group(4));
            return new AlarmTarget.WallTime(ZonedDateTime.of(date, time, now.getZone()));
        }
        if (TIME.matcher(input).matches()) {
            LocalTime time = parseTime(input);
            LocalDateTime dateTime = LocalDateTime.of(now.toLocalDate(), time);
            if (!dateTime.isAfter(now.toLocalDateTime())) {
                dateTime = dateTime.plusDays(1);
            }
            return new AlarmTarget.WallTime(dateTime.atZone(now.getZone()));
        }
        throw new IllegalArgumentException("invalid time: " + raw);
    }

    public static String formatWall(ZonedDateTime time, DateOrder dateOrder) {
        LocalDate date = time.toLocalDate();
        LocalTime localTime = time.toLocalTime();
        String datePart = dateOrder == DateOrder.MONTH_DAY
            ? "%d/%d/%d".formatted(date.getMonthValue(), date.getDayOfMonth(), date.getYear())
            : "%d/%d/%d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        int millis = localTime.getNano() / 1_000_000;
        return "%s %02d:%02d:%02d.%03d".formatted(
            datePart,
            localTime.getHour(),
            localTime.getMinute(),
            localTime.getSecond(),
            millis
        );
    }

    private static LocalTime parseTime(String raw) {
        Matcher matcher = TIME.matcher(raw.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("invalid time of day: " + raw);
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        int second = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        String meridiem = matcher.group(4);
        if (meridiem != null) {
            String normalized = meridiem.toLowerCase(Locale.ROOT).replace(".", "");
            if (hour < 1 || hour > 12) {
                throw new IllegalArgumentException("invalid hour: " + hour);
            }
            if (normalized.startsWith("p") && hour != 12) {
                hour += 12;
            } else if (normalized.startsWith("a") && hour == 12) {
                hour = 0;
            }
        } else if (hour > 23) {
            throw new IllegalArgumentException("invalid hour: " + hour);
        }
        return LocalTime.of(hour, minute, second);
    }

    private static LocalDate resolveSlashDate(int first, int second, Integer year, DateOrder dateOrder, LocalDate today) {
        int month;
        int day;
        if (dateOrder == DateOrder.MONTH_DAY) {
            month = first;
            day = second;
        } else {
            day = first;
            month = second;
        }
        if (year == null) {
            LocalDate candidate = LocalDate.of(today.getYear(), month, day);
            if (candidate.isBefore(today)) {
                candidate = candidate.plusYears(1);
            }
            return candidate;
        }
        return LocalDate.of(year, month, day);
    }

    private static int normalizeYear(int year, int currentYear) {
        if (year < 100) {
            int century = currentYear / 100 * 100;
            int mapped = century + year;
            if (mapped + 50 < currentYear) {
                mapped += 100;
            } else if (mapped - 50 > currentYear) {
                mapped -= 100;
            }
            return mapped;
        }
        return year;
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(ZoneId.systemDefault());
    }
}
