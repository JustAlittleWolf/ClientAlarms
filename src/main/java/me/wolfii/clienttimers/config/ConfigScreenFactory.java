package me.wolfii.clienttimers.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import me.wolfii.clienttimers.notify.SoundPlayer;
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
        Config config = Config.get();
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("clientalarms.config.title"))
            .save(() -> {
                SoundPlayer.stopPreview(Minecraft.getInstance());
                config.save();
            })
            .category(messages(config))
            .category(overlay(config))
            .category(sound(config))
            .build()
            .generateScreen(parent);
    }

    private static ConfigCategory messages(Config config) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("clientalarms.config.messages"))
            .option(bool("messageOnComplete", () -> config.messageOnComplete, value -> config.messageOnComplete = value, true))
            .option(bool("messageOnStart", () -> config.messageOnStart, value -> config.messageOnStart = value, true))
            .option(bool("messageOnInfo", () -> config.messageOnInfo, value -> config.messageOnInfo = value, true))
            .option(Option.<MessageDisplay>createBuilder()
                .name(Component.translatable("clientalarms.config.messageDisplay"))
                .description(OptionDescription.of(Component.translatable("clientalarms.config.messageDisplay.desc")))
                .binding(MessageDisplay.CHAT, () -> config.messageDisplay, value -> config.messageDisplay = value)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MessageDisplay.class))
                .build())
            .option(Option.<DateOrder>createBuilder()
                .name(Component.translatable("clientalarms.config.dateOrder"))
                .description(OptionDescription.of(Component.translatable("clientalarms.config.dateOrder.desc")))
                .binding(DateOrder.MONTH_DAY, () -> config.dateOrder, value -> config.dateOrder = value)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(DateOrder.class))
                .build())
            .option(string("alarmEndedFormat", () -> config.alarmEndedFormat, value -> config.alarmEndedFormat = value, ""))
            .option(string("timerEndedFormat", () -> config.timerEndedFormat, value -> config.timerEndedFormat = value, ""))
            .option(string("stopwatchStoppedFormat", () -> config.stopwatchStoppedFormat, value -> config.stopwatchStoppedFormat = value, ""))
            .option(string("alarmStartedFormat", () -> config.alarmStartedFormat, value -> config.alarmStartedFormat = value, ""))
            .option(string("timerStartedFormat", () -> config.timerStartedFormat, value -> config.timerStartedFormat = value, ""))
            .option(string("stopwatchStartedFormat", () -> config.stopwatchStartedFormat, value -> config.stopwatchStartedFormat = value, ""))
            .build();
    }

    private static ConfigCategory overlay(Config config) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("clientalarms.config.overlay"))
            .option(bool("overlayByDefault", () -> config.overlayByDefault, value -> config.overlayByDefault = value, true))
            .option(bool("screenActionButtons", () -> config.screenActionButtons, value -> config.screenActionButtons = value, true))
            .option(bool("silentByDefault", () -> config.silentByDefault, value -> config.silentByDefault = value, false))
            .option(Option.<Integer>createBuilder()
                .name(Component.translatable("clientalarms.config.autoStopAfterRings"))
                .description(OptionDescription.of(Component.translatable("clientalarms.config.autoStopAfterRings.desc")))
                .binding(0, () -> config.autoStopAfterRings, value -> config.autoStopAfterRings = value)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 20).step(1))
                .build())
            .group(overlayGroup("alarm", config.alarmOverlay))
            .group(overlayGroup("timer", config.timerOverlay))
            .group(overlayGroup("stopwatch", config.stopwatchOverlay))
            .build();
    }

    private static OptionGroup overlayGroup(String key, OverlaySettings settings) {
        return OptionGroup.createBuilder()
            .name(Component.translatable("clienttimers.config.overlay." + key))
            .option(bool("overlayEnabled", () -> settings.enabled, value -> settings.enabled = value, true))
            .option(bool("showHeading", () -> settings.showHeading, value -> settings.showHeading = value, true))
            .option(string("heading", () -> settings.heading, value -> settings.heading = value, ""))
            .option(string("overlayFormat", () -> settings.format, value -> settings.format = value, ""))
            .option(string("nameFormat", () -> settings.nameFormat, value -> settings.nameFormat = value, "%name%"))
            .option(Option.<OverlayAnchorX>createBuilder()
                .name(Component.translatable("clientalarms.config.anchorX"))
                .binding(OverlayAnchorX.LEFT, () -> settings.anchorX, value -> settings.anchorX = value)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(OverlayAnchorX.class))
                .build())
            .option(Option.<OverlayAnchorY>createBuilder()
                .name(Component.translatable("clientalarms.config.anchorY"))
                .binding(OverlayAnchorY.TOP, () -> settings.anchorY, value -> settings.anchorY = value)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(OverlayAnchorY.class))
                .build())
            .option(Option.<Integer>createBuilder()
                .name(Component.translatable("clientalarms.config.offsetX"))
                .binding(8, () -> settings.offsetX, value -> settings.offsetX = value)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-400, 400).step(1))
                .build())
            .option(Option.<Integer>createBuilder()
                .name(Component.translatable("clientalarms.config.offsetY"))
                .binding(8, () -> settings.offsetY, value -> settings.offsetY = value)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-400, 400).step(1))
                .build())
            .option(Option.<TextAlign>createBuilder()
                .name(Component.translatable("clientalarms.config.align"))
                .binding(TextAlign.LEFT, () -> settings.align, value -> settings.align = value)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(TextAlign.class))
                .build())
            .build();
    }

    private static ConfigCategory sound(Config config) {
        String[] saveName = {""};
        List<String> presetNames = new ArrayList<>();
        refreshPresetNames(config, presetNames);
        return ConfigCategory.createBuilder()
            .name(Component.translatable("clientalarms.config.sound"))
            .option(bool("playSounds", () -> config.playSounds, value -> config.playSounds = value, true))
            .option(Option.<SoundVolumeMode>createBuilder()
                .name(Component.translatable("clientalarms.config.soundVolumeMode"))
                .description(OptionDescription.of(Component.translatable("clientalarms.config.soundVolumeMode.desc")))
                .binding(SoundVolumeMode.ALARM, () -> config.soundVolumeMode, value -> config.soundVolumeMode = value)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(SoundVolumeMode.class))
                .build())
            .option(Option.<Float>createBuilder()
                .name(Component.translatable("clientalarms.config.masterVolume"))
                .binding(1.0f, () -> config.masterVolume, value -> config.masterVolume = value)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.05f))
                .build())
            .option(Option.<Integer>createBuilder()
                .name(Component.translatable("clientalarms.config.silenceTicks"))
                .description(OptionDescription.of(Component.translatable("clientalarms.config.silenceTicks.desc")))
                .binding(40, () -> config.silenceTicksBetweenRepeats, value -> config.silenceTicksBetweenRepeats = value)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 200).step(1))
                .build())
            .option(Option.<String>createBuilder()
                .name(Component.translatable("clientalarms.config.preset"))
                .binding("Pling", () -> config.selectedPreset, config::applyPreset)
                .controller(opt -> CyclingListControllerBuilder.create(opt).values(presetNames))
                .build())
            .option(Option.<String>createBuilder()
                .name(Component.translatable("clientalarms.config.presetName"))
                .binding("", () -> saveName[0], value -> saveName[0] = value)
                .controller(StringControllerBuilder::create)
                .build())
            .option(ButtonOption.createBuilder()
                .name(Component.translatable("clientalarms.config.savePreset"))
                .action((screen, option) -> {
                    if (!saveName[0].isBlank()) {
                        config.saveCurrentAsPreset(saveName[0]);
                        refreshPresetNames(config, presetNames);
                    }
                })
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(Component.translatable("clientalarms.config.preview"))
                .description(OptionDescription.of(Component.translatable("clientalarms.config.preview.desc")))
                .binding(false, SoundPlayer::isPreviewing, value -> {
                    if (value) {
                        SoundPlayer.startPreview();
                    } else {
                        SoundPlayer.stopPreview(Minecraft.getInstance());
                    }
                })
                .controller(TickBoxControllerBuilder::create)
                .build())
            .option(LabelOption.create(Component.translatable("clientalarms.config.notes.help")))
            .group(ListOption.<String>createBuilder()
                .name(Component.translatable("clientalarms.config.notes"))
                .binding(encodeNotes(Config.defaultPresets().getFirst().notes), () -> encodeNotes(config.notes), values -> config.notes = decodeNotes(values))
                .controller(StringControllerBuilder::create)
                .initial("minecraft:block.note_block.pling,1.0,1.0,0")
                .build())
            .build();
    }

    private static void refreshPresetNames(Config config, List<String> presetNames) {
        presetNames.clear();
        for (SoundPreset preset : config.presets) {
            presetNames.add(preset.name);
        }
        if (presetNames.isEmpty()) {
            presetNames.add("Pling");
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

    private static Option<String> string(String key, Supplier<String> getter, Consumer<String> setter, String def) {
        return Option.<String>createBuilder()
            .name(Component.translatable("clienttimers.config." + key))
            .description(OptionDescription.of(Component.translatable("clienttimers.config." + key + ".desc")))
            .binding(def, getter, setter)
            .controller(StringControllerBuilder::create)
            .build();
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
        List<AlarmNote> notes = new ArrayList<>();
        for (String value : values) {
            notes.add(decodeNote(value));
        }
        return notes;
    }

    private static AlarmNote decodeNote(String value) {
        String raw = value == null ? "" : value.trim().replaceAll("\\s*\\[ERROR:[^\\]]*\\]\\s*$", "");
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
