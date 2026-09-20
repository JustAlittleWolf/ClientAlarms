package me.wolfii.clienttimers.config;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import me.wolfii.clienttimers.sound.SoundPlayer;
import me.wolfii.clienttimers.time.DateOrder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigScreenFactory {
    private ConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        Config config = Config.getConfig();
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("clienttimers.config.title"))
            .save(() -> {
                SoundPlayer.stopPreview(Minecraft.getInstance());
                config.save();
            })
            .category(messages(config))
            .category(sound(config))
            .build()
            .generateScreen(parent);
    }

    private static ConfigCategory messages(Config config) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("clienttimers.config.messages"))
            .option(bool("messageOnComplete", () -> config.messageOnComplete, value -> config.messageOnComplete = value, true))
            .option(bool("messageOnStart", () -> config.messageOnStart, value -> config.messageOnStart = value, true))
            .option(bool("messageOnInfo", () -> config.messageOnInfo, value -> config.messageOnInfo = value, true))
            .option(enumerated("messageDisplay", MessageDisplay.class, MessageDisplay.CHAT, () -> config.messageDisplay, value -> config.messageDisplay = value, true))
            .option(enumerated("dateOrder", DateOrder.class, DateOrder.MONTH_DAY, () -> config.dateOrder, value -> config.dateOrder = value, true))
            .option(bool("silentByDefault", () -> config.silentByDefault, value -> config.silentByDefault = value, false))
            .option(intSlider("autoStopAfterRings", 0, 20, 0, () -> config.autoStopAfterRings, value -> config.autoStopAfterRings = value, true))
            .build();
    }

    private static ConfigCategory sound(Config config) {
        String[] saveName = {""};
        boolean[] syncing = {false};
        List<String> presetNames = new ArrayList<>();
        refreshPresetNames(config, presetNames);
        SoundPreset current = config.selected();

        ListOption<String> notes = ListOption.<String>createBuilder()
            .name(Component.translatable("clienttimers.config.notes"))
            .binding(encodeNotes(current.notes), () -> encodeNotes(config.selected().notes), values -> config.selected().setNotes(decodeNotes(values)))
            .controller(StringControllerBuilder::create)
            .initial("minecraft:block.note_block.pling,1.0,1.0,0")
            .listener((option, values) -> {
                if (!syncing[0]) {
                    config.selected().setNotes(decodeNotes(values));
                }
            })
            .build();

        Option<Integer> silence = Option.<Integer>createBuilder()
            .name(Component.translatable("clienttimers.config.silenceTicks"))
            .description(OptionDescription.of(Component.translatable("clienttimers.config.silenceTicks.desc")))
            .binding(40, () -> config.selected().silenceTicks, value -> config.selected().silenceTicks = value)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 200).step(1))
            .instant(true)
            .build();

        Option<String> preset = Option.<String>createBuilder()
            .name(Component.translatable("clienttimers.config.preset"))
            .description(OptionDescription.of(Component.translatable("clienttimers.config.preset.desc")))
            .binding(presetNames.getFirst(), () -> config.selectedPreset, config::selectPreset)
            .controller(opt -> CyclingListControllerBuilder.create(opt)
                .values(presetNames)
                .formatValue(Component::literal))
            .instant(true)
            .listener((option, name) -> {
                syncing[0] = true;
                config.selectPreset(name);
                SoundPreset selected = config.selected();
                silence.requestSet(selected.silenceTicks);
                notes.requestSet(encodeNotes(selected.notes));
                syncing[0] = false;
            })
            .build();

        return ConfigCategory.createBuilder()
            .name(Component.translatable("clienttimers.config.sound"))
            .option(bool("playSounds", () -> config.playSounds, value -> config.playSounds = value, true))
            .option(enumerated("soundVolumeMode", SoundVolumeMode.class, SoundVolumeMode.ALARM, () -> config.soundVolumeMode, value -> config.soundVolumeMode = value, true))
            .option(Option.<Float>createBuilder()
                .name(Component.translatable("clienttimers.config.masterVolume"))
                .binding(1.0f, () -> config.masterVolume, value -> config.masterVolume = value)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.05f))
                .build())
            .option(preset)
            .option(silence)
            .option(Option.<String>createBuilder()
                .name(Component.translatable("clienttimers.config.presetName"))
                .binding("", () -> saveName[0], value -> saveName[0] = value)
                .controller(StringControllerBuilder::create)
                .build())
            .option(ButtonOption.createBuilder()
                .name(Component.translatable("clienttimers.config.savePreset"))
                .action((screen, option) -> {
                    if (saveName[0].isBlank()) {
                        return;
                    }
                    config.selected().silenceTicks = silence.pendingValue();
                    config.selected().setNotes(decodeNotes(notes.pendingValue()));
                    config.saveCurrentAsPreset(saveName[0]);
                    refreshPresetNames(config, presetNames);
                    preset.requestSet(config.selectedPreset);
                })
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(Component.translatable("clienttimers.config.preview"))
                .description(OptionDescription.of(Component.translatable("clienttimers.config.preview.desc")))
                .binding(false, SoundPlayer::isPreviewing, value -> {
                    if (value) {
                        SoundPlayer.startPreview();
                    } else {
                        SoundPlayer.stopPreview(Minecraft.getInstance());
                    }
                })
                .controller(TickBoxControllerBuilder::create)
                .build())
            .option(LabelOption.create(Component.translatable("clienttimers.config.notes.help")))
            .group(notes)
            .build();
    }

    private static void refreshPresetNames(Config config, List<String> presetNames) {
        presetNames.clear();
        for (SoundPreset item : config.presets) {
            if (item.name != null && !item.name.isBlank()) {
                presetNames.add(item.name);
            }
        }
        if (presetNames.isEmpty()) {
            presetNames.add(config.selected().name);
        }
        if (config.selectedPreset == null || !presetNames.contains(config.selectedPreset)) {
            config.selectPreset(presetNames.getFirst());
        }
    }

    private static Option<Boolean> bool(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean def) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("clienttimers.config." + key))
            .description(OptionDescription.of(Component.translatable("clienttimers.config." + key + ".desc")))
            .binding(def, getter, setter)
            .controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
            .build();
    }

    private static Option<Integer> intSlider(
        String key,
        int min,
        int max,
        int def,
        Supplier<Integer> getter,
        Consumer<Integer> setter,
        boolean description
    ) {
        var builder = Option.<Integer>createBuilder()
            .name(Component.translatable("clienttimers.config." + key))
            .binding(def, getter, setter)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(1));
        if (description) {
            describe(builder, key);
        }
        return builder.build();
    }

    private static <T extends Enum<T>> Option<T> enumerated(
        String key,
        Class<T> type,
        T def,
        Supplier<T> getter,
        Consumer<T> setter,
        boolean description
    ) {
        var builder = Option.<T>createBuilder()
            .name(Component.translatable("clienttimers.config." + key))
            .binding(def, getter, setter)
            .controller(opt -> EnumControllerBuilder.create(opt).enumClass(type));
        if (description) {
            describe(builder, key);
        }
        return builder.build();
    }

    private static <T> void describe(Option.Builder<T> builder, String key) {
        builder.description(OptionDescription.of(Component.translatable("clienttimers.config." + key + ".desc")));
    }

    private static List<String> encodeNotes(List<AlarmNote> notes) {
        List<String> encoded = new ArrayList<>();
        for (AlarmNote note : notes) {
            encoded.add(encodeNote(note));
        }
        return encoded;
    }

    private static String encodeNote(AlarmNote note) {
        String base = "%s,%s,%s,%d".formatted(note.soundId, note.volume, note.pitch, note.tick);
        String error = note.validationError();
        if (error == null) {
            return base;
        }
        return base + "  [ERROR: " + error + "]";
    }

    private static List<AlarmNote> decodeNotes(List<String> values) {
        List<AlarmNote> decoded = new ArrayList<>();
        for (String value : values) {
            decoded.add(decodeNote(value));
        }
        return decoded;
    }

    private static AlarmNote decodeNote(String value) {
        String raw = value == null ? "" : value.trim().replaceAll("\\s*\\[ERROR:[^]]*]\\s*$", "");
        String[] parts = raw.split(",");
        AlarmNote note = new AlarmNote();
        if (parts.length > 0) {
            note.soundId = parts[0].trim();
        }
        try {
            if (parts.length > 1) {
                note.volume = Float.parseFloat(parts[1].trim());
            }
            if (parts.length > 2) {
                note.pitch = Float.parseFloat(parts[2].trim());
            }
            if (parts.length > 3) {
                note.tick = Integer.parseInt(parts[3].trim());
            }
        } catch (NumberFormatException exception) {
            note.tick = -1;
        }
        return note;
    }
}
