package me.wolfii.clienttimers.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import me.wolfii.clienttimers.time.DateOrder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
        .id(Identifier.parse("clienttimers:config"))
        .serializer(config -> GsonConfigSerializerBuilder.create(config)
            .setPath(FabricLoader.getInstance().getConfigDir().resolve("clienttimers.json"))
            .build())
        .build();

    static {
        HANDLER.load();
        getConfig().ensureDefaults();
    }

    @SerialEntry
    public boolean messageOnComplete = true;
    @SerialEntry
    public boolean messageOnStart = true;
    @SerialEntry
    public boolean messageOnInfo = true;
    @SerialEntry
    public MessageDisplay messageDisplay = MessageDisplay.CHAT;

    @SerialEntry
    public DateOrder dateOrder = DateOrder.DAY_MONTH;

    @SerialEntry
    public boolean silentByDefault = false;
    @SerialEntry
    public int autoStopAfterRings = 0;

    @SerialEntry
    public boolean playSounds = true;
    @SerialEntry
    public SoundVolumeMode soundVolumeMode = SoundVolumeMode.ALARM;
    @SerialEntry
    public int silenceTicksBetweenRepeats = 40;
    @SerialEntry
    public float masterVolume = 1.0f;
    @SerialEntry
    public List<AlarmNote> notes = new ArrayList<>();
    @SerialEntry
    public List<SoundPreset> presets = new ArrayList<>();
    @SerialEntry
    public String selectedPreset = "Beep";

    public static Config getConfig() {
        return HANDLER.instance();
    }

    public static Screen createScreen(Screen parent) {
        return ConfigScreenFactory.create(parent);
    }

    public static List<SoundPreset> defaultPresets() {
        List<SoundPreset> defaults = new ArrayList<>();
        defaults.add(new SoundPreset("Beep", 40, List.of(
            new AlarmNote("minecraft:block.note_block.pling", 1.0f, 1.0f, 0),
            new AlarmNote("minecraft:block.note_block.pling", 1.0f, 1.0f, 4),
            new AlarmNote("minecraft:block.note_block.pling", 1.0f, 1.0f, 8)
        )));
        defaults.add(new SoundPreset("Bell", 30, List.of(
            new AlarmNote("minecraft:block.note_block.bell", 1.0f, 1.0f, 0)
        )));
        return defaults;
    }

    public void save() {
        syncLegacyFields();
        HANDLER.save();
    }

    public void ensureDefaults() {
        if (notes == null) {
            notes = new ArrayList<>();
        }
        if (presets == null) {
            presets = new ArrayList<>();
        }
        if (presets.isEmpty()) {
            presets.addAll(defaultPresets());
        }
        for (SoundPreset preset : presets) {
            if (preset.notes == null) {
                preset.notes = new ArrayList<>();
            } else {
                preset.setNotes(preset.notes);
            }
        }
        if (!notes.isEmpty()) {
            selected().setNotes(notes);
            selected().silenceTicks = silenceTicksBetweenRepeats;
        }
        selectPreset(selectedPreset);
        syncLegacyFields();
    }

    public SoundPreset selected() {
        SoundPreset found = findPreset(selectedPreset);
        if (found != null) {
            return found;
        }
        if (presets.isEmpty()) {
            presets.addAll(defaultPresets());
        }
        selectedPreset = presets.getFirst().name;
        return presets.getFirst();
    }

    public SoundPreset findPreset(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (SoundPreset preset : presets) {
            if (preset.name != null && preset.name.equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return null;
    }

    public void selectPreset(String name) {
        SoundPreset found = findPreset(name);
        if (found != null) {
            selectedPreset = found.name;
            syncLegacyFields();
        }
    }

    public void saveCurrentAsPreset(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        SoundPreset created = selected().copy();
        created.name = name.trim();
        presets.removeIf(existing -> existing.name != null && existing.name.equalsIgnoreCase(created.name));
        presets.add(created);
        selectedPreset = created.name;
        syncLegacyFields();
    }

    public int cycleLengthTicks() {
        SoundPreset preset = selected();
        int last = 0;
        boolean any = false;
        for (AlarmNote note : preset.notes) {
            if (!note.isPlayable()) {
                continue;
            }
            last = Math.max(last, note.tick);
            any = true;
        }
        if (!any) {
            return Math.max(1, preset.silenceTicks);
        }
        return last + Math.max(0, preset.silenceTicks);
    }

    private void syncLegacyFields() {
        SoundPreset preset = selected();
        silenceTicksBetweenRepeats = preset.silenceTicks;
        notes = new ArrayList<>();
        for (AlarmNote note : preset.notes) {
            notes.add(note.copy());
        }
    }
}
