package dev.wellopti.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
	@WrapOperation(
		method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"
		)
	)
	private void wellopti$skipDistantEquipment(
		RenderLayer<?, ?> layer,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int lightCoords,
		EntityRenderState state,
		float yRot,
		float xRot,
		Operation<Void> original
	) {
		if (isEquipment(layer) && Culling.shouldSkipEquipment(state.distanceToCameraSq, state instanceof AvatarRenderState)) {
			return;
		}
		original.call(layer, poseStack, submitNodeCollector, lightCoords, state, yRot, xRot);
	}

	private static boolean isEquipment(RenderLayer<?, ?> layer) {
		return layer instanceof HumanoidArmorLayer
			|| layer instanceof ItemInHandLayer
			|| layer instanceof CustomHeadLayer
			|| layer instanceof WingsLayer
			|| layer instanceof SimpleEquipmentLayer;
	}
}
