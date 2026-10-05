package dev.wellopti.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wellopti.Culling;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Far-away mobs skip their equipment layers. Wool, slime goo, villager outfits and other body layers always draw. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	/** CameraRenderState moved package in 26.1, so both signatures are listed; on any given version exactly one exists. */
	@WrapWithCondition(
		method = {
			"submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			"submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"
		},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"
		)
	)
	private boolean wellopti$skipDistantEquipment(
		RenderLayer<?, ?> layer,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int lightCoords,
		EntityRenderState state,
		float yRot,
		float xRot
	) {
		// Returning false skips this one layer's draw call. No wrapper object is created per call.
		return !(isEquipment(layer) && Culling.shouldSkipEquipment(state.distanceToCameraSq, state instanceof AvatarRenderState));
	}

	private static boolean isEquipment(RenderLayer<?, ?> layer) {
		return layer instanceof HumanoidArmorLayer
			|| layer instanceof ItemInHandLayer
			|| layer instanceof CustomHeadLayer
			|| layer instanceof WingsLayer
			|| layer instanceof SimpleEquipmentLayer;
	}
}
