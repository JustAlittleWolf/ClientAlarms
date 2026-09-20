package me.wolfii.clienttimers.notify;

import dev.isxander.yacl3.gui.YACLScreen;
import me.wolfii.clienttimers.client.ClientTimersClient;
import me.wolfii.clienttimers.config.AlarmNote;
import me.wolfii.clienttimers.config.Config;
import me.wolfii.clienttimers.config.SoundVolumeMode;
import me.wolfii.clienttimers.engine.Trackable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class SoundPlayer {
    private static final List<SoundInstance> PREVIEW = new ArrayList<>();
    private static boolean previewing;
    private static int previewTick;

    private SoundPlayer() {
    }

    public static void tick(Minecraft minecraft, List<Trackable> ringing) {
        if (previewing) {
            if (!isSoundCategoryOpen(minecraft)) {
                stopPreview(minecraft);
            } else {
                tickCycle(minecraft, previewTick++, true);
                return;
            }
        }
        for (Trackable trackable : ringing) {
            if (!trackable.ringing || trackable.snoozeUntilEpoch > System.currentTimeMillis()) {
                continue;
            }
            if (trackable.soundCycleTick < 0) {
                trackable.soundCycleTick = 0;
            }
            if (shouldPlay(trackable)) {
                tickCycle(minecraft, trackable.soundCycleTick, false);
            }
            trackable.soundCycleTick++;
            int length = Math.max(1, Config.getConfig().cycleLengthTicks());
            if (trackable.soundCycleTick >= length) {
                trackable.soundCycleTick = 0;
                trackable.ringsCompleted++;
                int maxRings = Config.getConfig().autoStopAfterRings;
                if (maxRings > 0 && trackable.ringsCompleted >= maxRings) {
                    trackable.ringing = false;
                    if (!trackable.running) {
                        trackable.completed = true;
                    }
                }
            }
        }
    }

    public static void startPreview() {
        previewing = true;
        previewTick = 0;
    }

    public static void stopPreview(Minecraft minecraft) {
        previewing = false;
        previewTick = 0;
        Iterator<SoundInstance> iterator = PREVIEW.iterator();
        while (iterator.hasNext()) {
            minecraft.getSoundManager().stop(iterator.next());
            iterator.remove();
        }
    }

    public static boolean isPreviewing() {
        return previewing;
    }

    private static boolean shouldPlay(Trackable trackable) {
        if (!trackable.ringing || trackable.silent || !Config.getConfig().playSounds) {
            return false;
        }
        return trackable.snoozeUntilEpoch <= System.currentTimeMillis();
    }

    private static void tickCycle(Minecraft minecraft, int cycleTick, boolean preview) {
        if (!preview && !Config.getConfig().playSounds) {
            return;
        }
        for (AlarmNote note : Config.getConfig().notes) {
            if (!note.isPlayable()) {
                continue;
            }
            if (note.tick == cycleTick) {
                play(minecraft, note, preview);
            }
        }
    }

    private static void play(Minecraft minecraft, AlarmNote note, boolean preview) {
        Optional<SoundEvent> event = resolveKnown(note.soundId);
        if (event.isEmpty()) {
            ClientTimersClient.LOGGER.debug("Unknown alarm sound {}", note.soundId);
            return;
        }
        float volume = Math.max(0.0f, note.volume) * Config.getConfig().masterVolume;
        SoundInstance instance = Config.getConfig().soundVolumeMode == SoundVolumeMode.ALARM
            ? new AlarmSoundInstance(event.get(), note.pitch, volume)
            : SimpleSoundInstance.forUI(event.get(), note.pitch, volume);
        minecraft.getSoundManager().play(instance);
        if (preview) {
            PREVIEW.add(instance);
        }
    }

    public static Optional<SoundEvent> resolveKnown(String id) {
        Identifier identifier = parseSoundId(id);
        if (identifier == null) {
            return Optional.empty();
        }
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(identifier);
        return Optional.ofNullable(event);
    }

    public static Identifier parseSoundId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Identifier.parse(id.contains(":") ? id.trim() : "minecraft:" + id.trim());
        } catch (Exception exception) {
            return null;
        }
    }

    private static boolean isSoundCategoryOpen(Minecraft minecraft) {
        Screen screen = minecraft.screen;
        if (screen == null) {
            return false;
        }
        if (screen instanceof YACLScreen yacl) {
            return isSoundTab(yacl);
        }
        String className = screen.getClass().getName();
        return className.contains("yacl") || className.contains("YACL") || className.contains("PopupController");
    }

    private static boolean isSoundTab(YACLScreen screen) {
        Tab tab = screen.tabManager.getCurrentTab();
        if (tab == null) {
            return false;
        }
        String title = tab.getTabTitle().getString();
        String sound = Component.translatable("clienttimers.config.sound").getString();
        return title.contains(sound);
    }
}
