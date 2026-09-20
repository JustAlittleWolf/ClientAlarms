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
    public int autoStopAfterRings = 3;

    @SerialEntry
    public boolean playSounds = true;
    @SerialEntry
    public SoundVolumeMode soundVolumeMode = SoundVolumeMode.ALARM;
    @SerialEntry
    public int silenceTicksBetweenRepeats = 30;
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

    public void save() {
        HANDLER.save();
    }

    public void ensureDefaults() {
        if (notes == null) {
            notes = new ArrayList<>();
        }
        if (presets == null) {
            presets = new ArrayList<>();
        }
        PresetStore.initialize(this);
        List<String> names = PresetStore.names();
        if (selectedPreset == null || PresetStore.get(selectedPreset) == null) {
            selectedPreset = names.isEmpty() ? "Beep" : names.getFirst();
        }
    }

    public SoundPreset selected() {
        SoundPreset found = PresetStore.get(selectedPreset);
        if (found != null) {
            return found;
        }
        List<String> names = PresetStore.names();
        if (!names.isEmpty()) {
            selectedPreset = names.getFirst();
            found = PresetStore.get(selectedPreset);
            if (found != null) {
                return found;
            }
        }
        return new SoundPreset("Beep", 40, List.of(
            new AlarmNote("minecraft:block.note_block.pling", 1.0f, 1.0f, 0)
        ));
    }

    public void selectPreset(String name) {
        if (PresetStore.get(name) != null) {
            selectedPreset = name;
        }
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
}
