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
        sendChat(TimerMessages.started(trackable));
    }

    public static void ended(Trackable trackable, boolean force) {
        boolean soundsOff = !Config.getConfig().playSounds;
        if (!force && !Config.getConfig().messageOnComplete && !soundsOff && !trackable.silent) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        MessageDisplay display = Config.getConfig().messageDisplay;
        if (display == MessageDisplay.CHAT) {
            sendChat(TimerMessages.withStopAndSnooze(trackable, TimerMessages.ended(trackable)));
            return;
        }
        Component compact = TimerMessages.endedCompact(trackable);
        if (display == MessageDisplay.ACTIONBAR) {
            minecraft.player.sendOverlayMessage(compact);
            return;
        }
        minecraft.gui.setTimes(10, 40, 10);
        minecraft.gui.setTitle(compact);
        minecraft.gui.setSubtitle(Component.empty());
    }

    public static void info(Component component) {
        if (!Config.getConfig().messageOnInfo) {
            return;
        }
        sendChat(component);
    }

    public static void deferred(Component component) {
        sendChat(component);
    }

    public static void list(Component component) {
        sendChat(component);
    }

    private static void sendChat(Component component) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        minecraft.gui.getChat().addClientSystemMessage(component);
    }
}
