package me.wolfii.clienttimers.config;

import java.util.List;

public class SoundPreset {
    public String name;
    public int silenceTicks;
    public List<AlarmNote> notes;

    public SoundPreset(String name, int silenceTicks, List<AlarmNote> notes) {
        this.name = name;
        this.silenceTicks = silenceTicks;
        this.notes = notes;
    }
}
