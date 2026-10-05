package dev.wellopti.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wellopti.Culling;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	/**
	 * Shortens the name tag distance for mobs. Name tags are text, which is surprisingly costly to draw.
	 * The call lives in extractNameTags on 26.2+ and in extractRenderState on 26.1. Both are listed with full
	 * signatures (each name has overloads); on any given version exactly one of them exists.
	 */
	@WrapOperation(
		method = {
			"extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
			"extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"
		},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z"
		)
	)
	private boolean wellopti$nameTagDistance(EntityRenderer<?, ?> renderer, Entity entity, double distanceToCameraSq, Operation<Boolean> original) {
		return original.call(renderer, entity, distanceToCameraSq) && !Culling.isNameTagTooFar(entity, distanceToCameraSq);
	}
}
