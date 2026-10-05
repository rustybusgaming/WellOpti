package dev.wellopti.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.mojang.blaze3d.platform.Window;
import dev.wellopti.WellOptiClient;
import dev.wellopti.config.WellOptiConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dynamic FPS: don't burn the GPU on a window nobody is looking at. */
@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
	private void wellopti$throttleInactiveWindow(CallbackInfoReturnable<Integer> cir) {
		WellOptiConfig.DynamicFps cfg = WellOptiConfig.get().dynamicFps;
		if (!cfg.enabled || !WellOptiClient.active) {
			return;
		}

		Window window = this.minecraft.getWindow();
		int limit = cir.getReturnValueI();
		if (window.isIconified()) {
			cir.setReturnValue(Math.min(limit, cfg.minimizedFps));
		} else if (!window.isFocused()) {
			cir.setReturnValue(Math.min(limit, cfg.unfocusedFps));
		} else if (cfg.pausedFps > 0 && this.minecraft.isPaused()) {
			cir.setReturnValue(Math.min(limit, cfg.pausedFps));
		}
	}
}
