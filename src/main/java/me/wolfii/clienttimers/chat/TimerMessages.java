package me.wolfii.clienttimers.chat;

import me.wolfii.clienttimers.hud.OverlayText;
import me.wolfii.clienttimers.time.ClockMode;
import me.wolfii.clienttimers.timer.StoppedNotice;
import me.wolfii.clienttimers.timer.Trackable;
import me.wolfii.clienttimers.timer.TrackableKind;
import me.wolfii.clienttimers.world.WorldScope;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class TimerMessages {
    private TimerMessages() {
    }

    public static Component started(Trackable trackable) {
        return switch (trackable.kind) {
            case ALARM -> ChatStyle.wording(
                "clienttimers.message.alarmStarted",
                kind(trackable.kind),
                ChatStyle.name(trackable.name),
                ChatStyle.count(OverlayText.target(trackable))
            );
            case TIMER -> timerStarted(trackable);
            case STOPWATCH -> withExtras(
                ChatStyle.wording("clienttimers.message.started", kind(trackable.kind), ChatStyle.name(trackable.name)),
                trackable
            );
        };
    }

    public static Component ended(Trackable trackable) {
        MutableComponent message = switch (trackable.kind) {
            case ALARM -> ChatStyle.wording(
                "clienttimers.message.alarmEnded",
                kind(trackable.kind),
                ChatStyle.name(trackable.name),
                ChatStyle.count(OverlayText.target(trackable))
            );
            case TIMER -> ChatStyle.wording(
                "clienttimers.message.timerEnded",
                kind(trackable.kind),
                ChatStyle.name(trackable.name),
                ChatStyle.duration(OverlayText.elapsed(trackable))
            );
            case STOPWATCH -> ChatStyle.wording(
                "clienttimers.message.stopwatchStopped",
                kind(trackable.kind),
                ChatStyle.name(trackable.name),
                ChatStyle.duration(OverlayText.elapsed(trackable))
            );
        };
        if (trackable.missed) {
            message.append(ChatStyle.wording("clienttimers.message.missed"));
        }
        return message;
    }

    public static Component progress(Trackable trackable) {
        Component value = trackable.kind == TrackableKind.STOPWATCH
            ? ChatStyle.duration(OverlayText.elapsed(trackable))
            : ChatStyle.wording("clienttimers.message.remaining", ChatStyle.duration(OverlayText.remaining(trackable)));
        return ChatStyle.wording(
            "clienttimers.message.progress",
            kind(trackable.kind),
            ChatStyle.name(trackable.name),
            value
        );
    }

    public static Component listHeader(TrackableKind kind) {
        return ChatStyle.wording("clienttimers.list.header", kindPlural(kind));
    }

    public static Component listEmpty(TrackableKind kind) {
        return ChatStyle.wording("clienttimers.list.empty", kindPlural(kind));
    }

    public static Component listLine(Trackable trackable) {
        Component value = trackable.kind == TrackableKind.STOPWATCH
            ? ChatStyle.duration(OverlayText.elapsed(trackable))
            : ChatStyle.wording("clienttimers.message.remaining", ChatStyle.duration(OverlayText.remaining(trackable)));
        return ChatStyle.wording(
            "clienttimers.list.line",
            ChatStyle.name(trackable.name),
            value
        );
    }

    public static Component stopped(Trackable trackable) {
        return ChatStyle.wording("clienttimers.message.stopped", kind(trackable.kind), ChatStyle.name(trackable.name));
    }

    public static Component stoppedAll(int count) {
        return ChatStyle.wording("clienttimers.message.stoppedAll", ChatStyle.count(count));
    }

    public static Component stoppedOnLeave(StoppedNotice notice) {
        return ChatStyle.wording("clienttimers.message.stoppedOnLeave", kind(notice.kind), ChatStyle.name(notice.name));
    }

    public static Component silent(Trackable trackable) {
        return ChatStyle.wording(
            trackable.silent ? "clienttimers.message.silentOn" : "clienttimers.message.silentOff",
            kind(trackable.kind),
            ChatStyle.name(trackable.name)
        );
    }

    public static Component overlayVisibility(Trackable trackable) {
        return ChatStyle.wording(
            trackable.overlayVisible ? "clienttimers.message.shown" : "clienttimers.message.hidden",
            kind(trackable.kind),
            ChatStyle.name(trackable.name)
        );
    }

    public static Component snoozed(String duration) {
        return ChatStyle.wording("clienttimers.message.snoozed", ChatStyle.duration(duration));
    }

    public static Component kind(TrackableKind kind) {
        return Component.translatable("clienttimers.kind." + kind.id());
    }

    public static Component withStopAndSnooze(Trackable trackable, Component message) {
        MutableComponent result = message.copy();
        result.append(Component.literal(" "));
        result.append(ChatStyle.commandButton("clienttimers.button.stop", stopCommand(trackable), "clienttimers.button.stop.hover"));
        result.append(Component.literal(" "));
        result.append(ChatStyle.commandButton("clienttimers.button.snooze", "/csnooze", "clienttimers.button.snooze.hover"));
        return result;
    }

    private static Component timerStarted(Trackable trackable) {
        MutableComponent message = ChatStyle.wording(
            "clienttimers.message.timerStarted",
            kind(trackable.kind),
            ChatStyle.name(trackable.name),
            ChatStyle.duration(OverlayText.duration(trackable))
        );
        return withExtras(message, trackable);
    }

    private static MutableComponent withExtras(MutableComponent message, Trackable trackable) {
        boolean defaultMode = trackable.clockMode == ClockMode.TIME_PLAYING;
        boolean thisWorld = trackable.worldScope == WorldScope.THIS_WORLD && trackable.clockMode.supportsWorldScope();
        if (!defaultMode) {
            message.append(ChatStyle.wording("clienttimers.message.using", ChatStyle.count(modeLabel(trackable.clockMode))));
        }
        if (thisWorld) {
            message.append(ChatStyle.wording("clienttimers.message.inThisWorld"));
        }
        if (trackable.kind == TrackableKind.TIMER && trackable.hasRepeat && trackable.remainingRepeats != 0) {
            message.append(ChatStyle.wording("clienttimers.message.repeat", ChatStyle.duration(OverlayText.repeatDuration(trackable))));
            if (trackable.remainingRepeats >= 0) {
                message.append(ChatStyle.wording("clienttimers.message.repeatTimes", ChatStyle.count(trackable.remainingRepeats)));
            }
        }
        return message;
    }

    private static Component kindPlural(TrackableKind kind) {
        return Component.translatable("clienttimers.heading." + kind.id());
    }

    private static String modeLabel(ClockMode mode) {
        return Component.translatable("clienttimers.mode." + mode.commandName()).getString();
    }

    private static String stopCommand(Trackable trackable) {
        String safeName = trackable.name.contains(" ") ? "\"" + trackable.name + "\"" : trackable.name;
        return switch (trackable.kind) {
            case ALARM -> "/calarm stop " + safeName;
            case TIMER -> "/ctimer stop " + safeName;
            case STOPWATCH -> "/cstopwatch stop " + safeName;
        };
    }
}
