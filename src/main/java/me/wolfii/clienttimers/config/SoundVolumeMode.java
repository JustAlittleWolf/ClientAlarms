package me.wolfii.clienttimers.config;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

public enum SoundVolumeMode implements NameableEnum {
    ALARM,
    GAME;

    @Override
    public Component getDisplayName() {
        return Component.translatable("clienttimers.config.soundVolumeMode." + this.name());
    }
}
