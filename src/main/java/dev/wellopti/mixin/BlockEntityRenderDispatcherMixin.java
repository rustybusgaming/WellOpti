package dev.wellopti.mixin;

import dev.wellopti.Culling;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
	@Shadow
	private Vec3 cameraPos;

	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void wellopti$cullDistantBlockEntities(
		BlockEntity blockEntity,
		float partialTicks,
		ModelFeatureRenderer.CrumblingOverlay breakProgress,
		boolean isGloballyRendered,
		CallbackInfoReturnable<Object> cir
	) {
		if (!isGloballyRendered && Culling.shouldCullBlockEntity(blockEntity, this.cameraPos)) {
			cir.setReturnValue(null);
		}
	}
}
