package me.wolfii.clienttimers.config;

import java.util.ArrayList;
import java.util.List;

public class SoundPreset {
    public String name = "Pling";
    public int silenceTicks = 40;
    public List<AlarmNote> notes = new ArrayList<>();

    public SoundPreset() {
    }

    public SoundPreset(String name, int silenceTicks, List<AlarmNote> notes) {
        this.name = name;
        this.silenceTicks = silenceTicks;
        this.notes = copyNotes(notes);
    }

    public SoundPreset copy() {
        return new SoundPreset(name, silenceTicks, notes);
    }

    public void setNotes(List<AlarmNote> notes) {
        this.notes = copyNotes(notes);
    }

    private static List<AlarmNote> copyNotes(List<AlarmNote> notes) {
        List<AlarmNote> copy = new ArrayList<>();
        if (notes == null) {
            return copy;
        }
        for (AlarmNote note : notes) {
            copy.add(note.copy());
        }
        return copy;
    }
}
