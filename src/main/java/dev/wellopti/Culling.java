package dev.wellopti;

import dev.wellopti.compat.EntityKinds;
import dev.wellopti.compat.Shaders;
import dev.wellopti.config.WellOptiConfig;
import dev.wellopti.occlusion.OcclusionCuller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.core.BlockPos;
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
	/** Mobs drawn so far this frame in each block, for the crowd limit. */
	private static final Long2IntOpenHashMap CROWD = new Long2IntOpenHashMap();

	private Culling() {
	}

	/** Drops per-world tables and shrinks them back to their starting size. */
	public static void clearCaches() {
		CROWD.clear();
		CROWD.trim();
	}

	/** Called at the start of each frame's entity pass. */
	public static void beginFrame() {
		// Keep the table's capacity between frames (no reallocation), but shrink it if one huge frame bloated it.
		if (CROWD.size() > 4096) {
			CROWD.clear();
			CROWD.trim();
		} else {
			CROWD.clear();
		}
		TickThrottle.beginFrame();
		OcclusionCuller.beginFrame();
	}

	/**
	 * Crowd limit: in a mob farm with 80 chickens in one block you can't tell 8 from 80, so stop drawing after N.
	 * Must run last, after every other check has decided the mob would be drawn.
	 */
	public static boolean isOverCrowdLimit(Entity entity) {
		int limit = WellOptiConfig.get().mobs.crowdLimit;
		if (limit <= 0 || !WellOptiClient.active || !(entity instanceof LivingEntity) || entity instanceof Player || isBoss(entity)
			|| Shaders.isRenderingShadowPass()) {
			return false;
		}

		int drawn = CROWD.addTo(entity.blockPosition().asLong(), 1);
		if (drawn >= limit) {
			WellOptiStats.crowded++;
			return true;
		}
		return false;
	}

	/** Far-away mobs skip their equipment layers (armor, held items, elytra, heads). Players always keep theirs. */
	public static boolean shouldSkipEquipment(double distanceToCameraSq, boolean isPlayer) {
		int limit = WellOptiConfig.get().mobs.equipmentDistance;
		return limit > 0 && WellOptiClient.active && !isPlayer && beyond(distanceToCameraSq, limit);
	}

	/** Shorter name tag distance for mobs; players keep vanilla's 64 blocks. */
	public static boolean isNameTagTooFar(Entity entity, double distanceToCameraSq) {
		int limit = WellOptiConfig.get().mobs.nameTagDistance;
		return limit > 0 && WellOptiClient.active && !(entity instanceof Player) && beyond(distanceToCameraSq, limit);
	}

	public static boolean shouldCullEntity(Entity entity, double camX, double camY, double camZ) {
		WellOptiConfig.EntityCulling cfg = WellOptiConfig.get().entityCulling;
		if (!cfg.enabled || !WellOptiClient.active) {
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
		if (!cfg.enabled || !WellOptiClient.active || cameraPos == null) {
			return false;
		}

		int limit = blockEntityLimit(blockEntity, cfg);
		if (limit <= 0) {
			return false;
		}

		BlockPos pos = blockEntity.getBlockPos();
		double dx = pos.getX() + 0.5 - cameraPos.x;
		double dy = pos.getY() + 0.5 - cameraPos.y;
		double dz = pos.getZ() + 0.5 - cameraPos.z;
		if (!beyond(dx * dx + dy * dy + dz * dz, limit) || (cfg.disableWhileScoping && isScoping())) {
			return false;
		}

		WellOptiStats.culledBlockEntities++;
		return true;
	}

	/**
	 * Called after vanilla has decided an entity is on screen: hides it anyway if it's entirely behind solid blocks.
	 * Players are never hidden this way, because vanilla shows their name tags through walls.
	 */
	public static boolean isEntityOccluded(Entity entity, double camX, double camY, double camZ) {
		WellOptiConfig.OcclusionCulling cfg = WellOptiConfig.get().occlusionCulling;
		// In a shader's shadow pass, "hidden from the camera" says nothing about whether the shadow is visible.
		if (!cfg.enabled || !cfg.entities || !WellOptiClient.active || Shaders.isRenderingShadowPass()) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		Entity camera = minecraft.getCameraEntity();
		if (level == null || entity instanceof Player || entity.isCurrentlyGlowing() || entity == camera) {
			return false;
		}

		// Leads are drawn from the leashed mob to its holder and can be visible when either end is hidden.
		if (entity instanceof Leashable leashable && leashable.isLeashed()) {
			return false;
		}

		if (camera != null && (entity.hasPassenger(camera) || camera.hasPassenger(entity))) {
			return false;
		}

		return OcclusionCuller.isEntityHidden(entity, level, camX, camY, camZ);
	}

	public static boolean isBlockEntityOccluded(BlockEntity blockEntity, Vec3 cameraPos) {
		WellOptiConfig.OcclusionCulling cfg = WellOptiConfig.get().occlusionCulling;
		if (!cfg.enabled || !cfg.blockEntities || !WellOptiClient.active || cameraPos == null || Shaders.isRenderingShadowPass()) {
			return false;
		}

		ClientLevel level = Minecraft.getInstance().level;
		return level != null && OcclusionCuller.isBlockEntityHidden(blockEntity.getBlockPos(), level, cameraPos);
	}

	private static int entityLimit(Entity entity, WellOptiConfig.EntityCulling cfg) {
		if (entity instanceof ItemEntity) return cfg.droppedItems;
		if (entity instanceof ExperienceOrb) return cfg.experienceOrbs;
		if (entity instanceof ItemFrame) return cfg.itemFrames;
		if (entity instanceof ArmorStand) return cfg.armorStands;
		if (EntityKinds.isArrow(entity)) return EntityKinds.isStuckArrow(entity) ? cfg.stuckArrows : 0;
		if (EntityKinds.isPainting(entity)) return cfg.paintings;
		// Minecarts and boats, unless something is riding them.
		if (entity instanceof VehicleEntity) return entity.isVehicle() ? 0 : cfg.vehicles;
		if (EntityKinds.isAmbientMob(entity)) return cfg.ambientMobs;
		if (entity instanceof Player || isBoss(entity) || entity.hasCustomName()) return 0;
		if (EntityKinds.isVillager(entity)) return cfg.villagers;
		if (entity instanceof Enemy) return cfg.hostileMobs;
		if (entity instanceof Animal) return cfg.passiveMobs;
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
		if (blockEntity instanceof SpawnerBlockEntity || blockEntity instanceof TrialSpawnerBlockEntity || blockEntity instanceof VaultBlockEntity) {
			return cfg.spawners;
		}
		return 0;
	}

	private static boolean isBoss(Entity entity) {
		return entity instanceof EnderDragon || entity instanceof WitherBoss;
	}

	private static boolean isScoping() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && player.isScoping();
	}

	/** True when something {@code distanceSqr} away is beyond {@code limit} blocks, after adaptive mode's scaling. */
	private static boolean beyond(double distanceSqr, int limit) {
		double scaled = limit * AdaptiveDistance.scale();
		return distanceSqr > scaled * scaled;
	}
}
