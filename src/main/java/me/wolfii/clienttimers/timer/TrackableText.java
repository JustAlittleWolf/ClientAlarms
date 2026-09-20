package me.wolfii.clienttimers.timer;

import me.wolfii.clienttimers.time.ClockMode;
import me.wolfii.clienttimers.time.DurationParser;
import me.wolfii.clienttimers.world.WorldKeys;
import net.minecraft.client.Minecraft;

public final class TrackableText {
    private TrackableText() {
    }

    public static String elapsed(Trackable trackable) {
        if (trackable.clockMode == ClockMode.TICKS_PLAYING) {
            return DurationParser.formatTicks(trackable.elapsedTicks);
        }
        return DurationParser.formatMillis(trackable.elapsedMillis);
    }

    public static String remaining(Trackable trackable) {
        if (trackable.kind == TrackableKind.STOPWATCH) {
            return elapsed(trackable);
        }
        if (trackable.clockMode == ClockMode.TICKS_PLAYING) {
            return DurationParser.formatTicks(Math.max(0, trackable.durationTicks - trackable.elapsedTicks));
        }
        if (trackable.kind == TrackableKind.ALARM) {
            Minecraft minecraft = Minecraft.getInstance();
            return switch (trackable.alarmType) {
                case "WALL" -> DurationParser.formatMillis(Math.max(0, trackable.targetEpochMillis - System.currentTimeMillis()));
                case "GAME_TIME" -> DurationParser.formatTicks(Math.max(0, trackable.targetWorldTime - Math.max(0, WorldKeys.currentWorldTime(minecraft))));
                case "GAME_DAY" -> {
                    long current = WorldKeys.currentWorldDay(minecraft);
                    long remainingDays = Math.max(0, trackable.targetDay - Math.max(0, current));
                    yield remainingDays + "d";
                }
                default -> DurationParser.formatMillis(Math.max(0, trackable.durationMillis - trackable.elapsedMillis));
            };
        }
        return DurationParser.formatMillis(Math.max(0, trackable.durationMillis - trackable.elapsedMillis));
    }

    public static String duration(Trackable trackable) {
        if (trackable.kind == TrackableKind.ALARM) {
            return trackable.targetDisplay == null ? "" : trackable.targetDisplay;
        }
        if (trackable.clockMode == ClockMode.TICKS_PLAYING) {
            return DurationParser.formatTicks(trackable.durationTicks);
        }
        return trackable.durationRaw == null || trackable.durationRaw.isBlank()
            ? DurationParser.formatMillis(trackable.durationMillis)
            : trackable.durationRaw;
    }

    public static String repeatDuration(Trackable trackable) {
        if (trackable.repeatRaw != null && !trackable.repeatRaw.isBlank()) {
            return trackable.repeatRaw;
        }
        if (trackable.clockMode == ClockMode.TICKS_PLAYING) {
            return DurationParser.formatTicks(trackable.repeatTicks);
        }
        return DurationParser.formatMillis(trackable.repeatMillis);
    }

    public static String target(Trackable trackable) {
        if (trackable.targetDisplay == null || trackable.targetDisplay.isBlank()) {
            return duration(trackable);
        }
        return trackable.targetDisplay;
    }
}
