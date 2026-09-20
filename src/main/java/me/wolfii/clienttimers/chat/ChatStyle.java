package me.wolfii.clienttimers.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

final class ChatStyle {
    /** Names of alarms, timers and stopwatches. */
    static final int NAME = 0x7CFF9A;
    /** Durations, remaining time and elapsed time. */
    static final int DURATION = 0x6EC8FF;
    /** Counts, clock modes and calendar timestamps. */
    static final int COUNT = 0xFFD166;
    static final int ACTION = 0x7CFF9A;

    private ChatStyle() {
    }

    static MutableComponent name(String name) {
        return Component.literal(name).withStyle(style -> style.withColor(rgb(NAME)));
    }

    static MutableComponent duration(Object value) {
        return data(value, DURATION);
    }

    static MutableComponent count(Object value) {
        return data(value, COUNT);
    }

    static MutableComponent data(Object value, int color) {
        return Component.literal(String.valueOf(value)).withStyle(style -> style.withColor(rgb(color)));
    }

    static MutableComponent wording(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    static MutableComponent commandButton(String labelKey, String command, String hoverKey) {
        return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.translatable(labelKey).withStyle(style -> style
                .withColor(rgb(ACTION))
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(wording(hoverKey)))))
            .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
    }

    static TextColor rgb(int color) {
        return TextColor.fromRgb(color);
    }
}
