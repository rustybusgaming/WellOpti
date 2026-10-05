package dev.wellopti.mixin;

import dev.wellopti.bench.Benchmark;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** One call per frame: the benchmark measures the time between these. */
	@Inject(method = "runTick", at = @At("HEAD"))
	private void wellopti$onFrame(boolean advanceGameTime, CallbackInfo ci) {
		Benchmark.onFrame();
	}
}
