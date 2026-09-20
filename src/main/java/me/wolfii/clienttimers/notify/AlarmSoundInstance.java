package me.wolfii.clienttimers.notify;

import me.wolfii.clienttimers.config.Config;
import me.wolfii.clienttimers.config.SoundVolumeMode;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class AlarmSoundInstance extends SimpleSoundInstance {
    public AlarmSoundInstance(SoundEvent event, float pitch, float volume) {
        super(
            event.location(),
            SoundSource.UI,
            volume,
            pitch,
            SoundInstance.createUnseededRandom(),
            false,
            0,
            Attenuation.NONE,
            0.0,
            0.0,
            0.0,
            true
        );
    }

    public static boolean ignoresGameVolume(SoundInstance instance) {
        return instance instanceof AlarmSoundInstance && Config.get().soundVolumeMode == SoundVolumeMode.ALARM;
    }
}
