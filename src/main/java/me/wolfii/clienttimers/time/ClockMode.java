package me.wolfii.clienttimers.time;

public enum ClockMode {
    TICKS_PLAYING,
    TIME_PLAYING,
    GAME_RUNNING,
    REAL_TIME;

    public static ClockMode fromCommand(String value) {
        return ClockMode.valueOf(value.toUpperCase());
    }

    public String commandName() {
        return name().toLowerCase();
    }

    public boolean supportsWorldScope() {
        return this == TICKS_PLAYING || this == TIME_PLAYING;
    }
}
