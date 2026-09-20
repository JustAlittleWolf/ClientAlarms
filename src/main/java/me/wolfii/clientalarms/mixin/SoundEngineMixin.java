package me.wolfii.clientalarms.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.wolfii.clientalarms.notify.AlarmSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @WrapOperation(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"
            )
    )
    private float clientalarms$playWithAlarmVolume(
            SoundEngine instance,
            float volume,
            SoundSource source,
            Operation<Float> original,
            SoundInstance sound
    ) {
        if (AlarmSoundInstance.ignoresGameVolume(sound)) {
            return Mth.clamp(volume, 0.0F, 1.0F);
        }
        return original.call(instance, volume, source);
    }

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("HEAD"), cancellable = true)
    private void clientalarms$keepAlarmVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        if (AlarmSoundInstance.ignoresGameVolume(sound)) {
            cir.setReturnValue(Mth.clamp(sound.getVolume(), 0.0F, 1.0F));
        }
    }
}
