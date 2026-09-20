package me.wolfii.clientalarms.hud;

import me.wolfii.clientalarms.config.Config;
import me.wolfii.clientalarms.config.OverlaySettings;
import me.wolfii.clientalarms.config.TextAlign;
import me.wolfii.clientalarms.engine.AlarmEngine;
import me.wolfii.clientalarms.engine.Trackable;
import me.wolfii.clientalarms.engine.TrackableKind;
import me.wolfii.clientalarms.notify.MessageFormats;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public class OverlayHudElement implements HudElement {
    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker ignored) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        Font font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Map<TrackableKind, List<Trackable>> grouped = AlarmEngine.visibleOverlay();
        for (TrackableKind kind : TrackableKind.values()) {
            OverlaySettings settings = Config.get().overlayFor(kind);
            if (!settings.enabled) {
                continue;
            }
            List<Trackable> entries = grouped.get(kind);
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            int x = anchorX(settings, width);
            int y = anchorY(settings, height);
            if (settings.showHeading) {
                draw(graphics, font, MessageFormats.heading(kind), x, y, settings.align, 0xFFFFFF);
                y += font.lineHeight + 1;
            }
            for (Trackable entry : entries) {
                int color = entry.ringing ? 0xFF5555 : 0xFFFFFF;
                draw(graphics, font, MessageFormats.overlayLine(entry), x, y, settings.align, color);
                y += font.lineHeight + 1;
            }
        }
    }

    private static int anchorX(OverlaySettings settings, int width) {
        return switch (settings.anchorX) {
            case LEFT -> settings.offsetX;
            case CENTER -> width / 2 + settings.offsetX;
            case RIGHT -> width - settings.offsetX;
        };
    }

    private static int anchorY(OverlaySettings settings, int height) {
        return switch (settings.anchorY) {
            case TOP -> settings.offsetY;
            case CENTER -> height / 2 + settings.offsetY;
            case BOTTOM -> height - settings.offsetY;
        };
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, TextAlign align, int color) {
        int drawX = switch (align) {
            case LEFT -> x;
            case CENTER -> x - font.width(text) / 2;
            case RIGHT -> x - font.width(text);
        };
        graphics.text(font, Component.literal(text), drawX, y, color, true);
    }
}
