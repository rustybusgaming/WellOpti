package dev.wellopti.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wellopti.Culling;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The block entity is read with {@code @Local} because the method's other parameters differ between Minecraft versions. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
	@Shadow
	private Vec3 cameraPos;

	@Shadow
	public abstract <E extends BlockEntity> BlockEntityRenderer<E, ?> getRenderer(E blockEntity);

	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void wellopti$cullDistantBlockEntities(CallbackInfoReturnable<Object> cir, @Local(argsOnly = true) BlockEntity blockEntity) {
		if (Culling.shouldCullBlockEntity(blockEntity, this.cameraPos)) {
			cir.setReturnValue(null);
		}
	}

	/**
	 * Piggybacks on vanilla's own distance check, so we only raycast block entities that would otherwise be drawn.
	 * Block entities drawn regardless of the camera (beacon beams) are visible over walls and are left alone.
	 */
	@ModifyExpressionValue(
		method = "tryExtractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z"
		)
	)
	private boolean wellopti$cullOccludedBlockEntities(boolean shouldRender, @Local(argsOnly = true) BlockEntity blockEntity) {
		if (!shouldRender) {
			return false;
		}

		BlockEntityRenderer<BlockEntity, ?> renderer = this.getRenderer(blockEntity);
		if (renderer != null && renderer.shouldRenderOffScreen()) {
			return true;
		}
		return !Culling.isBlockEntityOccluded(blockEntity, this.cameraPos);
	}
}
