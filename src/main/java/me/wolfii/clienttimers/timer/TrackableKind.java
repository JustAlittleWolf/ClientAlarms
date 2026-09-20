package me.wolfii.clienttimers.timer;

import java.util.Locale;

public enum TrackableKind {
    ALARM,
    TIMER,
    STOPWATCH;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
