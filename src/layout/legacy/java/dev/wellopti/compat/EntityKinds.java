package dev.wellopti.compat;

import dev.wellopti.mixin.AbstractArrowAccessor;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.ambient.AmbientCreature;

/** Entity checks whose classes moved package between Minecraft versions. Minecraft 1.21.10 and earlier, before 1.21.11 moved these mobs into per-kind packages. */
public final class EntityKinds {
	private EntityKinds() {
	}

	/** An arrow or trident stuck in a block (not one in flight). */
	public static boolean isStuckArrow(Entity entity) {
		return entity instanceof AbstractArrow arrow && ((AbstractArrowAccessor) arrow).wellopti$isInGround();
	}

	public static boolean isArrow(Entity entity) {
		return entity instanceof AbstractArrow;
	}

	/** Bats, fish, tadpoles and squid. */
	public static boolean isAmbientMob(Entity entity) {
		return entity instanceof AmbientCreature || entity instanceof AbstractFish || entity instanceof Squid;
	}

	/** Villagers and wandering traders. */
	public static boolean isVillager(Entity entity) {
		return entity instanceof AbstractVillager;
	}

	public static boolean isPainting(Entity entity) {
		return entity instanceof Painting;
	}
}
