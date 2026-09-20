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
}
