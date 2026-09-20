package me.wolfii.clienttimers.time;

public record ParsedDuration(long extraTicks, long extraMillis, String raw) {
    public long ticksFor(ClockMode mode) {
        if (mode == ClockMode.TICKS_PLAYING) {
            return extraTicks + extraMillis / 50L;
        }
        return extraTicks + Math.round(extraMillis / 50.0);
    }

    public long millis() {
        return extraTicks * 50L + extraMillis;
    }

    public boolean isZero() {
        return extraTicks <= 0 && extraMillis <= 0;
    }
}
