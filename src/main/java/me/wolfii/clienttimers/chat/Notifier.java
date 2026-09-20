package me.wolfii.clienttimers.chat;

import me.wolfii.clienttimers.config.Config;
import me.wolfii.clienttimers.config.MessageDisplay;
import me.wolfii.clienttimers.timer.Trackable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Notifier {
    private Notifier() {
    }

    public static void started(Trackable trackable) {
        if (!Config.getConfig().messageOnStart) {
            return;
        }
        send(TimerMessages.started(trackable), false);
    }

    public static void ended(Trackable trackable, boolean force) {
        boolean soundsOff = !Config.getConfig().playSounds;
        if (!force && !Config.getConfig().messageOnComplete && !soundsOff && !trackable.silent) {
            return;
        }
        Component message = TimerMessages.ended(trackable);
        boolean forceChat = force || trackable.silent || soundsOff;
        if (usesChat(forceChat)) {
            message = TimerMessages.withStopAndSnooze(trackable, message);
        }
        send(message, forceChat);
    }

    public static void info(Component component) {
        if (!Config.getConfig().messageOnInfo) {
            return;
        }
        send(component, false);
    }

    public static void deferred(Component component) {
        send(component, true);
    }

    public static void list(Component component) {
        Minecraft.getInstance().gui.getChat().addClientSystemMessage(component);
    }

    private static boolean usesChat(boolean forceChat) {
        Minecraft minecraft = Minecraft.getInstance();
        return forceChat || Config.getConfig().messageDisplay == MessageDisplay.CHAT || minecraft.player == null;
    }

    private static void send(Component component, boolean forceChat) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (usesChat(forceChat)) {
            minecraft.gui.getChat().addClientSystemMessage(component);
            return;
        }
        if (Config.getConfig().messageDisplay == MessageDisplay.ACTIONBAR) {
            minecraft.player.sendOverlayMessage(component);
            return;
        }
        minecraft.gui.setTimes(10, 40, 10);
        minecraft.gui.setTitle(component);
        minecraft.gui.setSubtitle(ChatStyle.wording("clienttimers.subtitle.actions"));
    }
}
