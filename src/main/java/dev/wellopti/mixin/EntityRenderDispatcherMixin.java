package dev.wellopti.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.wellopti.Culling;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Arguments are read with {@code @Local} rather than listed, because the method's parameter list differs
 * between Minecraft versions (26.2 added a partial-tick argument).
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void wellopti$cullDistantEntities(
		CallbackInfoReturnable<Boolean> cir,
		@Local(argsOnly = true) Entity entity,
		@Local(argsOnly = true, ordinal = 0) double camX,
		@Local(argsOnly = true, ordinal = 1) double camY,
		@Local(argsOnly = true, ordinal = 2) double camZ
	) {
		if (Culling.shouldCullEntity(entity, camX, camY, camZ)) {
			cir.setReturnValue(false);
		}
	}

	/**
	 * Runs after vanilla's view-cone check, so we only raycast for entities that would otherwise be drawn.
	 * The crowd limit goes last so it only counts mobs that really are about to be drawn.
	 */
	@Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
	private void wellopti$cullOccludedAndCrowdedEntities(
		CallbackInfoReturnable<Boolean> cir,
		@Local(argsOnly = true) Entity entity,
		@Local(argsOnly = true, ordinal = 0) double camX,
		@Local(argsOnly = true, ordinal = 1) double camY,
		@Local(argsOnly = true, ordinal = 2) double camZ
	) {
		if (cir.getReturnValueZ() && (Culling.isEntityOccluded(entity, camX, camY, camZ) || Culling.isOverCrowdLimit(entity))) {
			cir.setReturnValue(false);
		}
	}
}
