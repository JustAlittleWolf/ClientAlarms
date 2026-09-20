package me.wolfii.clienttimers.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
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
        PresetStore.reload();
        config.ensureDefaults();
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
        List<String> presetNames = new ArrayList<>(PresetStore.names());
        if (presetNames.isEmpty()) {
            presetNames.add("Beep");
        }
        if (!containsIgnoreCase(presetNames, config.selectedPreset)) {
            config.selectedPreset = presetNames.getFirst();
        }
        return ConfigCategory.createBuilder()
            .name(Component.translatable("clienttimers.config.sound"))
            .option(bool("playSounds", () -> config.playSounds, value -> config.playSounds = value, true))
            .option(enumerated("soundVolumeMode", SoundVolumeMode.class, SoundVolumeMode.ALARM, () -> config.soundVolumeMode, value -> config.soundVolumeMode = value, true))
            .option(Option.<Float>createBuilder()
                .name(Component.translatable("clienttimers.config.masterVolume"))
                .binding(1.0f, () -> config.masterVolume, value -> config.masterVolume = value)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.05f))
                .build())
            .option(Option.<String>createBuilder()
                .name(Component.translatable("clienttimers.config.preset"))
                .description(OptionDescription.of(Component.translatable("clienttimers.config.preset.desc")))
                .binding(presetNames.getFirst(), () -> config.selectedPreset, config::selectPreset)
                .controller(opt -> DropdownStringControllerBuilder.create(opt)
                    .values(presetNames)
                    .allowEmptyValue(false)
                    .allowAnyValue(false))
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
            .option(LabelOption.create(Component.translatable("clienttimers.config.preset.help")))
            .build();
    }

    private static boolean containsIgnoreCase(List<String> names, String value) {
        if (value == null) {
            return false;
        }
        for (String name : names) {
            if (name.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
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
}
