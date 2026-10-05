package dev.wellopti;

import dev.wellopti.config.WellOptiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Decides which far-away things aren't worth drawing. Everything here is purely visual:
 * the game still ticks these objects, we just skip building and submitting their geometry.
 */
public final class Culling {
	private Culling() {
	}

	public static boolean shouldCullEntity(Entity entity, double camX, double camY, double camZ) {
		WellOptiConfig.EntityCulling cfg = WellOptiConfig.get().entityCulling;
		if (!cfg.enabled) {
			return false;
		}

		int limit = entityLimit(entity, cfg);
		if (limit <= 0 || entity.isCurrentlyGlowing()) {
			return false;
		}

		if (!beyond(entity.distanceToSqr(camX, camY, camZ), limit) || (cfg.disableWhileScoping && isScoping())) {
			return false;
		}

		WellOptiStats.culledEntities++;
		return true;
	}

	public static boolean shouldCullBlockEntity(BlockEntity blockEntity, Vec3 cameraPos) {
		WellOptiConfig.BlockEntityCulling cfg = WellOptiConfig.get().blockEntityCulling;
		if (!cfg.enabled || cameraPos == null) {
			return false;
		}

		int limit = blockEntityLimit(blockEntity, cfg);
		if (limit <= 0) {
			return false;
		}

		if (!beyond(Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(cameraPos), limit) || (cfg.disableWhileScoping && isScoping())) {
			return false;
		}

		WellOptiStats.culledBlockEntities++;
		return true;
	}

	private static int entityLimit(Entity entity, WellOptiConfig.EntityCulling cfg) {
		if (entity instanceof ItemEntity) return cfg.droppedItems;
		if (entity instanceof ExperienceOrb) return cfg.experienceOrbs;
		if (entity instanceof ItemFrame) return cfg.itemFrames;
		if (entity instanceof ArmorStand) return cfg.armorStands;
		return 0;
	}

	private static int blockEntityLimit(BlockEntity blockEntity, WellOptiConfig.BlockEntityCulling cfg) {
		// Signs are block models these days; their renderer only draws the text.
		if (blockEntity instanceof SignBlockEntity) return cfg.signText;
		if (blockEntity instanceof BannerBlockEntity) return cfg.banners;
		if (blockEntity instanceof SkullBlockEntity) return cfg.skulls;
		if (blockEntity instanceof LidBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) return cfg.storage;
		if (blockEntity instanceof ShelfBlockEntity || blockEntity instanceof CampfireBlockEntity || blockEntity instanceof DecoratedPotBlockEntity) {
			return cfg.itemDisplays;
		}
		return 0;
	}

	private static boolean isScoping() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && player.isScoping();
	}

	/** True when something {@code distanceSqr} away is beyond {@code limit} blocks. */
	private static boolean beyond(double distanceSqr, int limit) {
		return distanceSqr > (double) limit * limit;
	}
}
