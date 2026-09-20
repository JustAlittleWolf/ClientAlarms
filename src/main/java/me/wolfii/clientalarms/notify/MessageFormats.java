package me.wolfii.clientalarms.notify;

import me.wolfii.clientalarms.config.Config;
import me.wolfii.clientalarms.engine.Trackable;
import me.wolfii.clientalarms.engine.TrackableKind;
import me.wolfii.clientalarms.time.ClockMode;
import me.wolfii.clientalarms.time.DurationParser;
import me.wolfii.clientalarms.time.WorldKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;

public final class MessageFormats {
    private MessageFormats() {
    }

    public static Component formatEnded(Trackable trackable) {
        String pattern = switch (trackable.kind) {
            case ALARM -> firstNonBlank(Config.get().alarmEndedFormat, Component.translatable("clientalarms.format.alarmEnded").getString());
            case TIMER -> firstNonBlank(Config.get().timerEndedFormat, Component.translatable("clientalarms.format.timerEnded").getString());
            case STOPWATCH -> firstNonBlank(Config.get().stopwatchStoppedFormat, Component.translatable("clientalarms.format.stopwatchStopped").getString());
        };
        return Component.literal(apply(pattern, placeholders(trackable)));
    }

    public static Component formatStarted(Trackable trackable) {
        String pattern = switch (trackable.kind) {
            case ALARM -> firstNonBlank(Config.get().alarmStartedFormat, Component.translatable("clientalarms.format.alarmStarted").getString());
            case TIMER -> firstNonBlank(Config.get().timerStartedFormat, Component.translatable("clientalarms.format.timerStarted").getString());
            case STOPWATCH -> firstNonBlank(Config.get().stopwatchStartedFormat, Component.translatable("clientalarms.format.stopwatchStarted").getString());
        };
        return Component.literal(apply(pattern, placeholders(trackable)));
    }

    public static String overlayLine(Trackable trackable) {
        var overlay = Config.get().overlayFor(trackable.kind);
        String name = apply(firstNonBlank(overlay.nameFormat, "%name%"), placeholders(trackable));
        Map<String, String> values = placeholders(trackable);
        values.put("name", name);
        String pattern = firstNonBlank(overlay.format, defaultOverlayPattern(trackable.kind));
        return apply(pattern, values);
    }

    public static String heading(TrackableKind kind) {
        var overlay = Config.get().overlayFor(kind);
        return firstNonBlank(overlay.heading, Component.translatable("clientalarms.heading." + kind.name().toLowerCase()).getString());
    }

    public static Map<String, String> placeholders(Trackable trackable) {
        Map<String, String> values = new HashMap<>();
        values.put("name", trackable.name);
        values.put("elapsed", elapsed(trackable));
        values.put("remaining", remaining(trackable));
        values.put("duration", duration(trackable));
        values.put("target", trackable.targetDisplay == null || trackable.targetDisplay.isBlank() ? duration(trackable) : trackable.targetDisplay);
        values.put("mode", trackable.clockMode.commandName());
        values.put("status", status(trackable));
        return values;
    }

    public static String apply(String pattern, Map<String, String> values) {
        String result = pattern;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public static MutableComponent colored(Component component) {
        return component.copy().withStyle(ChatFormatting.YELLOW);
    }

    private static String elapsed(Trackable trackable) {
        if (trackable.clockMode == ClockMode.TICKS_PLAYING) {
            return DurationParser.formatTicks(trackable.elapsedTicks);
        }
        return DurationParser.formatMillis(trackable.elapsedMillis);
    }

    private static String remaining(Trackable trackable) {
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

    private static String duration(Trackable trackable) {
        if (trackable.kind == TrackableKind.ALARM) {
            return trackable.targetDisplay;
        }
        if (trackable.clockMode == ClockMode.TICKS_PLAYING) {
            return DurationParser.formatTicks(trackable.durationTicks);
        }
        return trackable.durationRaw == null || trackable.durationRaw.isBlank()
                ? DurationParser.formatMillis(trackable.durationMillis)
                : trackable.durationRaw;
    }

    private static String status(Trackable trackable) {
        if (trackable.snoozeUntilEpoch > System.currentTimeMillis()) {
            return "snoozed";
        }
        if (trackable.ringing) {
            return "ringing";
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
        return Component.translatable("clientalarms.format.overlay." + kind.name().toLowerCase()).getString();
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
