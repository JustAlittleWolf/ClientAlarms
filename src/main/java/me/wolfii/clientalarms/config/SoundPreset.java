package me.wolfii.clientalarms.config;

import java.util.ArrayList;
import java.util.List;

public class SoundPreset {
    public String name = "custom";
    public int silenceTicks = 20;
    public List<AlarmNote> notes = new ArrayList<>();

    public SoundPreset() {
    }

    public SoundPreset(String name, int silenceTicks, List<AlarmNote> notes) {
        this.name = name;
        this.silenceTicks = silenceTicks;
        this.notes = notes;
    }

    public SoundPreset copy() {
        List<AlarmNote> copied = new ArrayList<>();
        for (AlarmNote note : notes) {
            copied.add(note.copy());
        }
        return new SoundPreset(name, silenceTicks, copied);
    }
}
