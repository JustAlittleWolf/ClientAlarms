package me.wolfii.clienttimers.timer;

import java.time.ZonedDateTime;

public sealed interface AlarmTarget permits AlarmTarget.GameDay, AlarmTarget.GameTime, AlarmTarget.WallTime {
    record WallTime(ZonedDateTime when) implements AlarmTarget {
    }

    record GameTime(long worldTime) implements AlarmTarget {
    }

    record GameDay(long day) implements AlarmTarget {
    }
}
