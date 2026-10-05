package dev.wellopti.mixin;

import dev.wellopti.Culling;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
	@Inject(method = "extractVisibleEntities", at = @At("HEAD"))
	private void wellopti$beginEntityPass(CallbackInfo ci) {
		Culling.beginFrame();
	}
}
