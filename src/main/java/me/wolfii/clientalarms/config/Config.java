package me.wolfii.clientalarms.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import me.wolfii.clientalarms.time.DateOrder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
            .id(Identifier.parse("clientalarms:config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("clientalarms.json"))
                    .build())
            .build();

    static {
        HANDLER.load();
        get().ensureDefaults();
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
    public String alarmEndedFormat = "";
    @SerialEntry
    public String timerEndedFormat = "";
    @SerialEntry
    public String stopwatchStoppedFormat = "";
    @SerialEntry
    public String alarmStartedFormat = "";
    @SerialEntry
    public String timerStartedFormat = "";
    @SerialEntry
    public String stopwatchStartedFormat = "";

    @SerialEntry
    public DateOrder dateOrder = DateOrder.MONTH_DAY;

    @SerialEntry
    public OverlaySettings alarmOverlay = new OverlaySettings();
    @SerialEntry
    public OverlaySettings timerOverlay = OverlaySettings.at(8, 40);
    @SerialEntry
    public OverlaySettings stopwatchOverlay = OverlaySettings.at(8, 80);

    @SerialEntry
    public boolean overlayByDefault = true;
    @SerialEntry
    public boolean screenActionButtons = true;
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
    public String selectedPreset = "Pling";

    public static Config get() {
        return HANDLER.instance();
    }

    public static ConfigClassHandler<Config> handler() {
        return HANDLER;
    }

    public static Screen createScreen(Screen parent) {
        return ConfigScreenFactory.create(parent);
    }

    public void save() {
        HANDLER.save();
    }

    public OverlaySettings overlayFor(me.wolfii.clientalarms.engine.TrackableKind kind) {
        return switch (kind) {
            case ALARM -> alarmOverlay;
            case TIMER -> timerOverlay;
            case STOPWATCH -> stopwatchOverlay;
        };
    }

    public void ensureDefaults() {
        if (alarmOverlay == null) alarmOverlay = new OverlaySettings();
        if (timerOverlay == null) timerOverlay = new OverlaySettings();
        if (stopwatchOverlay == null) stopwatchOverlay = new OverlaySettings();
        if (notes == null) notes = new ArrayList<>();
        if (presets == null) presets = new ArrayList<>();
        if (presets.isEmpty()) {
            presets.addAll(defaultPresets());
        }
        if (notes.isEmpty()) {
            applyPreset(selectedPreset);
        }
    }

    public static List<SoundPreset> defaultPresets() {
        List<SoundPreset> defaults = new ArrayList<>();
        defaults.add(new SoundPreset("Pling", 40, List.of(
                new AlarmNote("minecraft:block.note_block.pling", 1.0f, 1.0f, 0),
                new AlarmNote("minecraft:block.note_block.pling", 1.0f, 1.5f, 6)
        )));
        defaults.add(new SoundPreset("Bell", 30, List.of(
                new AlarmNote("minecraft:block.note_block.bell", 1.0f, 0.8f, 0),
                new AlarmNote("minecraft:block.note_block.bell", 1.0f, 1.2f, 10)
        )));
        defaults.add(new SoundPreset("Bit", 20, List.of(
                new AlarmNote("minecraft:block.note_block.bit", 1.0f, 1.0f, 0),
                new AlarmNote("minecraft:block.note_block.bit", 1.0f, 1.2f, 4),
                new AlarmNote("minecraft:block.note_block.bit", 1.0f, 1.5f, 8)
        )));
        defaults.add(new SoundPreset("Chime", 50, List.of(
                new AlarmNote("minecraft:block.note_block.chime", 1.0f, 1.0f, 0),
                new AlarmNote("minecraft:block.note_block.chime", 0.8f, 1.4f, 8)
        )));
        defaults.add(new SoundPreset("Bass", 40, List.of(
                new AlarmNote("minecraft:block.note_block.bass", 1.0f, 0.7f, 0),
                new AlarmNote("minecraft:block.note_block.bass", 1.0f, 0.9f, 8)
        )));
        return defaults;
    }

    public void applyPreset(String name) {
        for (SoundPreset preset : presets) {
            if (preset.name.equalsIgnoreCase(name)) {
                selectedPreset = preset.name;
                silenceTicksBetweenRepeats = preset.silenceTicks;
                notes = new ArrayList<>();
                for (AlarmNote note : preset.notes) {
                    notes.add(note.copy());
                }
                return;
            }
        }
    }

    public void saveCurrentAsPreset(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        SoundPreset preset = new SoundPreset(name.trim(), silenceTicksBetweenRepeats, new ArrayList<>());
        for (AlarmNote note : notes) {
            preset.notes.add(note.copy());
        }
        presets.removeIf(existing -> existing.name.equalsIgnoreCase(preset.name));
        presets.add(preset);
        selectedPreset = preset.name;
    }

    public int cycleLengthTicks() {
        int last = 0;
        boolean any = false;
        for (AlarmNote note : notes) {
            if (!note.isPlayable()) {
                continue;
            }
            last = Math.max(last, note.tick);
            any = true;
        }
        if (!any) {
            return Math.max(1, silenceTicksBetweenRepeats);
        }
        return last + Math.max(0, silenceTicksBetweenRepeats);
    }
}
