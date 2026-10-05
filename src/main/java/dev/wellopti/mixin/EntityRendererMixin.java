package dev.wellopti.mixin;

import dev.wellopti.Culling;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	/** Shortens the 64-block name tag distance for mobs. Name tags are text, which is surprisingly costly to draw. */
	@ModifyArg(
		method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V"
		),
		index = 3
	)
	private double wellopti$nameTagDistance(Entity entity, EntityRenderState state, float partialTicks, double nameTagDistance, double belowNameDistance) {
		return Culling.nameTagDistance(entity, nameTagDistance);
	}
}
