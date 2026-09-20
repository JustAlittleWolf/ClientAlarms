package me.wolfii.clienttimers.notify;

import me.wolfii.clienttimers.config.Config;
import me.wolfii.clienttimers.config.MessageDisplay;
import me.wolfii.clienttimers.engine.Trackable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;

public final class Notifier {
    private Notifier() {
    }

    public static void started(Trackable trackable) {
        if (!Config.getConfig().messageOnStart) {
            return;
        }
        send(MessageFormats.colored(MessageFormats.formatStarted(trackable)), false);
    }

    public static void ended(Trackable trackable, boolean force) {
        boolean soundsOff = !Config.getConfig().playSounds;
        if (!force && !Config.getConfig().messageOnComplete && !soundsOff && !trackable.silent) {
            return;
        }
        MutableComponent message = MessageFormats.colored(MessageFormats.formatEnded(trackable));
        if (trackable.missed) {
            message.append(Component.literal(" ").append(Component.translatable("clienttimers.message.missed").withStyle(ChatFormatting.RED)));
        }
        message.append(Component.literal(" "));
        message.append(button(Component.translatable("clienttimers.button.stop"), stopCommand(trackable), "clienttimers.button.stop.hover"));
        message.append(Component.literal(" "));
        message.append(button(Component.translatable("clienttimers.button.snooze"), "/csnooze", "clienttimers.button.snooze.hover"));
        send(message, force || trackable.silent || soundsOff);
    }

    public static void info(Component component) {
        if (!Config.getConfig().messageOnInfo) {
            return;
        }
        send(MessageFormats.colored(component), false);
    }

    public static void list(Component component) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.getChat().addClientSystemMessage(MessageFormats.colored(component));
    }

    private static void send(Component component, boolean forceChat) {
        Minecraft minecraft = Minecraft.getInstance();
        MessageDisplay display = Config.getConfig().messageDisplay;
        if (forceChat || display == MessageDisplay.CHAT || minecraft.player == null) {
            minecraft.gui.getChat().addClientSystemMessage(component);
            return;
        }
        if (display == MessageDisplay.ACTIONBAR) {
            minecraft.player.sendOverlayMessage(component);
            return;
        }
        minecraft.gui.setTimes(10, 40, 10);
        minecraft.gui.setTitle(component);
        minecraft.gui.setSubtitle(Component.empty());
    }

    private static MutableComponent button(Component label, String command, String hoverKey) {
        return Component.literal("[").withStyle(ChatFormatting.GRAY)
            .append(label.copy().setStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable(hoverKey)))))
            .append(Component.literal("]").withStyle(ChatFormatting.GRAY));
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
