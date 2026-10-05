package dev.wellopti.mixin;

import dev.wellopti.Culling;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void wellopti$cullDistantEntities(
		Entity entity, Frustum culler, double camX, double camY, double camZ, float partialTicks, CallbackInfoReturnable<Boolean> cir
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
		Entity entity, Frustum culler, double camX, double camY, double camZ, float partialTicks, CallbackInfoReturnable<Boolean> cir
	) {
		if (cir.getReturnValueZ() && (Culling.isEntityOccluded(entity, camX, camY, camZ) || Culling.isOverCrowdLimit(entity))) {
			cir.setReturnValue(false);
		}
	}
}
