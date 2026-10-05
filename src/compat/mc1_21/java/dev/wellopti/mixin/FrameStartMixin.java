package dev.wellopti.mixin;

import dev.wellopti.Culling;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft 1.21.x: the per-frame entity pass lives in LevelRenderer. */
@Mixin(LevelRenderer.class)
public abstract class FrameStartMixin {
	@Inject(method = "extractVisibleEntities", at = @At("HEAD"))
	private void wellopti$beginEntityPass(CallbackInfo ci) {
		Culling.beginFrame();
	}
}
