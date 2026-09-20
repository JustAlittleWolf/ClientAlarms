package me.wolfii.clientalarms.notify;

import me.wolfii.clientalarms.ClientAlarms;
import me.wolfii.clientalarms.config.AlarmNote;
import me.wolfii.clientalarms.config.Config;
import me.wolfii.clientalarms.config.SoundVolumeMode;
import me.wolfii.clientalarms.engine.Trackable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
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
            if (!isConfigScreenOpen(minecraft)) {
                stopPreview(minecraft);
            } else {
                tickCycle(minecraft, previewTick++);
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
                tickCycle(minecraft, trackable.soundCycleTick);
            }
            trackable.soundCycleTick++;
            int length = Math.max(1, Config.get().cycleLengthTicks());
            if (trackable.soundCycleTick >= length) {
                trackable.soundCycleTick = 0;
                trackable.ringsCompleted++;
                int maxRings = Config.get().autoStopAfterRings;
                if (maxRings > 0 && trackable.ringsCompleted >= maxRings) {
                    trackable.ringing = false;
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
        if (minecraft.getSoundManager() == null) {
            return;
        }
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
        if (!trackable.ringing || trackable.silent || !Config.get().playSounds) {
            return false;
        }
        return trackable.snoozeUntilEpoch <= System.currentTimeMillis();
    }

    private static void tickCycle(Minecraft minecraft, int cycleTick) {
        if (!Config.get().playSounds || minecraft.getSoundManager() == null) {
            return;
        }
        for (AlarmNote note : Config.get().notes) {
            if (note.tick == cycleTick) {
                play(minecraft, note);
            }
        }
    }

    private static void play(Minecraft minecraft, AlarmNote note) {
        Optional<SoundEvent> event = resolve(note.soundId);
        if (event.isEmpty()) {
            ClientAlarms.LOGGER.debug("Unknown alarm sound {}", note.soundId);
            return;
        }
        float volume = Math.max(0.0f, note.volume) * Config.get().masterVolume;
        SoundInstance instance = Config.get().soundVolumeMode == SoundVolumeMode.ALARM
                ? new AlarmSoundInstance(event.get(), note.pitch, volume)
                : SimpleSoundInstance.forUI(event.get(), note.pitch, volume);
        minecraft.getSoundManager().play(instance);
        if (previewing) {
            PREVIEW.add(instance);
        }
    }

    private static Optional<SoundEvent> resolve(String id) {
        try {
            Identifier identifier = Identifier.parse(id.contains(":") ? id : "minecraft:" + id);
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(identifier);
            if (event != null) {
                return Optional.of(event);
            }
            return Optional.of(SoundEvent.createVariableRangeEvent(identifier));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static boolean isConfigScreenOpen(Minecraft minecraft) {
        return minecraft.screen != null && minecraft.screen.getClass().getName().contains("yacl");
    }
}
