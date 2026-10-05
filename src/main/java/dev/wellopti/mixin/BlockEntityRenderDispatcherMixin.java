package dev.wellopti.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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

	/**
	 * Piggybacks on vanilla's own distance check, so we only raycast block entities that would otherwise be drawn.
	 * Globally rendered ones (beacon beams) are visible over walls and are left alone.
	 */
	@ModifyExpressionValue(
		method = "tryExtractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z"
		)
	)
	private boolean wellopti$cullOccludedBlockEntities(
		boolean shouldRender,
		BlockEntity blockEntity,
		float partialTicks,
		ModelFeatureRenderer.CrumblingOverlay breakProgress,
		boolean isGloballyRendered
	) {
		return shouldRender && (isGloballyRendered || !Culling.isBlockEntityOccluded(blockEntity, this.cameraPos));
	}
}
