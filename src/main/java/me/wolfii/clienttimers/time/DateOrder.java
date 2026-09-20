package me.wolfii.clienttimers.time;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

public enum DateOrder implements NameableEnum {
    MONTH_DAY,
    DAY_MONTH;

    @Override
    public Component getDisplayName() {
        return Component.translatable("clienttimers.config.dateOrder." + this.name());
    }
}
