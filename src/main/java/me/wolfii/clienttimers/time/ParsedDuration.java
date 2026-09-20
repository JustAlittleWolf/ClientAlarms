package me.wolfii.clienttimers.time;

public record ParsedDuration(long extraTicks, long extraMillis, String raw) {
    public long ticksFor(ClockMode mode) {
        if (mode == ClockMode.TICKS_PLAYING) {
            return extraTicks + extraMillis / 50L;
        }
        return extraTicks + Math.round(extraMillis / 50.0);
    }

    public long millisFor(ClockMode mode) {
        if (mode == ClockMode.TICKS_PLAYING) {
            return extraTicks * 50L + extraMillis;
        }
        return extraTicks * 50L + extraMillis;
    }

    public boolean isZero() {
        return extraTicks <= 0 && extraMillis <= 0;
    }

    public ParsedDuration plus(ParsedDuration other) {
        return new ParsedDuration(extraTicks + other.extraTicks, extraMillis + other.extraMillis, raw + other.raw);
    }
}
