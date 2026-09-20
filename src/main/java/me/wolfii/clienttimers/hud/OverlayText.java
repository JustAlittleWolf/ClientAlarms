package me.wolfii.clienttimers.hud;

import me.wolfii.clienttimers.config.Config;
import me.wolfii.clienttimers.time.ClockMode;
import me.wolfii.clienttimers.time.DurationParser;
import me.wolfii.clienttimers.timer.Trackable;
import me.wolfii.clienttimers.timer.TrackableKind;
import me.wolfii.clienttimers.world.WorldKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public final class OverlayText {
    private OverlayText() {
    }

    public static String overlayLine(Trackable trackable) {
        var overlay = Config.getConfig().overlayFor(trackable.kind);
        Map<String, String> values = placeholders(trackable);
        String name = apply(firstNonBlank(overlay.nameFormat, "%name%"), values);
        values.put("name", name);
        return apply(firstNonBlank(overlay.format, defaultOverlayPattern(trackable.kind)), values);
    }

    public static String heading(TrackableKind kind) {
        var overlay = Config.getConfig().overlayFor(kind);
        return firstNonBlank(overlay.heading, Component.translatable("clienttimers.heading." + kind.id()).getString());
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

    private static Map<String, String> placeholders(Trackable trackable) {
        Map<String, String> values = new HashMap<>();
        values.put("name", trackable.name);
        values.put("elapsed", elapsed(trackable));
        values.put("remaining", remaining(trackable));
        values.put("duration", duration(trackable));
        values.put("target", target(trackable));
        values.put("mode", trackable.clockMode.commandName());
        values.put("status", status(trackable));
        return values;
    }

    private static String apply(String pattern, Map<String, String> values) {
        String result = pattern;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static String status(Trackable trackable) {
        if (trackable.snoozeUntilEpoch > System.currentTimeMillis()) {
            return "snoozed";
        }
        if (trackable.ringing) {
            return "ringing";
        }
        if (trackable.pendingJoinRing) {
            return "pending";
        }
        if (trackable.completed) {
            return "done";
        }
        if (!trackable.running) {
            return "stopped";
        }
        return "running";
    }

    private static String defaultOverlayPattern(TrackableKind kind) {
        return switch (kind) {
            case ALARM -> "%name%: %remaining% left (%target%)";
            case TIMER -> "%name%: %remaining% left";
            case STOPWATCH -> "%name%: %elapsed%";
        };
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
