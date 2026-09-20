package me.wolfii.clientalarms.hud;

import me.wolfii.clientalarms.config.Config;
import me.wolfii.clientalarms.engine.AlarmEngine;
import me.wolfii.clientalarms.time.DurationParser;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

public final class ScreenAlarmButtons {
    private ScreenAlarmButtons() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof ChatScreen) {
                return;
            }
            Button stop = Button.builder(Component.translatable("clientalarms.button.stop"), button -> AlarmEngine.stopAllRinging())
                    .bounds(4, 4, 72, 20)
                    .tooltip(Tooltip.create(Component.translatable("clientalarms.button.stopAll.hover")))
                    .build();
            Button snooze = Button.builder(Component.translatable("clientalarms.button.snooze"), button -> AlarmEngine.snoozeAll(DurationParser.parse("5min")))
                    .bounds(80, 4, 100, 20)
                    .tooltip(Tooltip.create(Component.translatable("clientalarms.button.snooze.hover")))
                    .build();
            stop.visible = false;
            snooze.visible = false;
            Screens.getWidgets(screen).add(stop);
            Screens.getWidgets(screen).add(snooze);
            ScreenEvents.afterTick(screen).register(current -> update(stop, snooze));
            update(stop, snooze);
        });
    }

    private static void update(Button stop, Button snooze) {
        boolean show = Config.get().screenActionButtons && AlarmEngine.hasRinging();
        stop.visible = show;
        snooze.visible = show;
        stop.active = show;
        snooze.active = show;
    }
}
