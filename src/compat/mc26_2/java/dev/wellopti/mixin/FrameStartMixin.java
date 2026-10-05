package dev.wellopti.mixin;

import dev.wellopti.Culling;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft 26.2+: the per-frame entity pass lives in LevelExtractor. */
@Mixin(LevelExtractor.class)
public abstract class FrameStartMixin {
	@Inject(method = "extractVisibleEntities", at = @At("HEAD"))
	private void wellopti$beginEntityPass(CallbackInfo ci) {
		Culling.beginFrame();
	}
}
