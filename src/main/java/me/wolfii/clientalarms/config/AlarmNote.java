package me.wolfii.clientalarms.config;

public class AlarmNote {
    public String soundId = "minecraft:block.note_block.pling";
    public float volume = 1.0f;
    public float pitch = 1.0f;
    public int tick = 0;

    public AlarmNote() {
    }

    public AlarmNote(String soundId, float volume, float pitch, int tick) {
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
        this.tick = tick;
    }

    public AlarmNote copy() {
        return new AlarmNote(soundId, volume, pitch, tick);
    }

    public String validationError() {
        if (soundId == null || soundId.isBlank()) {
            return "empty sound";
        }
        if (tick < 0) {
            return "negative tick";
        }
        if (volume < 0 || Float.isNaN(volume)) {
            return "invalid volume";
        }
        if (pitch <= 0 || Float.isNaN(pitch)) {
            return "invalid pitch";
        }
        if (me.wolfii.clientalarms.notify.SoundPlayer.resolveKnown(soundId).isEmpty()) {
            return "unknown sound";
        }
        return null;
    }

    public boolean isPlayable() {
        return validationError() == null;
    }
}
