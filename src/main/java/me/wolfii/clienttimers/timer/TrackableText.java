package me.wolfii.clienttimers.timer;

import me.wolfii.clienttimers.time.ClockMode;
import me.wolfii.clienttimers.time.DurationParser;
import me.wolfii.clienttimers.world.WorldKeys;
import net.minecraft.client.Minecraft;

public final class TrackableText {
    private TrackableText() {
    }

    public static String elapsed(Trackable trackable) {
        return format(trackable, trackable.elapsedMillis, trackable.elapsedTicks);
    }

    public static String remaining(Trackable trackable) {
        if (trackable.kind == TrackableKind.STOPWATCH) {
            return elapsed(trackable);
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
                default -> format(trackable, Math.max(0, trackable.durationMillis - trackable.elapsedMillis), Math.max(0, trackable.durationTicks - trackable.elapsedTicks));
            };
        }
        return format(
            trackable,
            Math.max(0, trackable.durationMillis - trackable.elapsedMillis),
            Math.max(0, trackable.durationTicks - trackable.elapsedTicks)
        );
    }

    public static String duration(Trackable trackable) {
        if (trackable.kind == TrackableKind.ALARM) {
            return trackable.targetDisplay == null ? "" : trackable.targetDisplay;
        }
        return format(trackable, trackable.durationMillis, trackable.durationTicks);
    }

    public static String repeatDuration(Trackable trackable) {
        return format(trackable, trackable.repeatMillis, trackable.repeatTicks);
    }

    public static String target(Trackable trackable) {
        if (trackable.targetDisplay == null || trackable.targetDisplay.isBlank()) {
            return duration(trackable);
        }
        return trackable.targetDisplay;
    }

    private static String format(Trackable trackable, long millis, long ticks) {
        if (trackable.clockMode == ClockMode.TICKS_PLAYING) {
            return DurationParser.formatPlayingTicks(ticks);
        }
        return DurationParser.formatMillis(millis);
    }
}
