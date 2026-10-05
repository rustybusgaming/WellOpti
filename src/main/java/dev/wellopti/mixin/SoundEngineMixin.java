package dev.wellopti.mixin;

import dev.wellopti.audio.BackgroundAudio;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Inject(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At("RETURN"), cancellable = true)
	private void wellopti$backgroundVolume(float volume, SoundSource source, CallbackInfoReturnable<Float> cir) {
		float multiplier = BackgroundAudio.multiplier();
		if (multiplier != 1.0F) {
			cir.setReturnValue(cir.getReturnValueF() * multiplier);
		}
	}
}
