package dev.wellopti.particle;

import dev.wellopti.WellOptiClient;
import dev.wellopti.config.WellOptiConfig;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Thins out purely decorative particles that blocks and biomes spawn constantly. Particles that tell you
 * something (hits, potion effects, explosions, block breaking, portals, redstone) are never touched.
 */
public final class AmbientParticles {
	private static final Set<ParticleType<?>> AMBIENT = new ReferenceOpenHashSet<>(new ParticleType<?>[] {
		ParticleTypes.CAMPFIRE_COSY_SMOKE, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
		ParticleTypes.RAIN, ParticleTypes.UNDERWATER, ParticleTypes.MYCELIUM,
		ParticleTypes.ASH, ParticleTypes.WHITE_ASH, ParticleTypes.CRIMSON_SPORE, ParticleTypes.WARPED_SPORE,
		ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.FALLING_SPORE_BLOSSOM, ParticleTypes.FIREFLY,
		ParticleTypes.CHERRY_LEAVES, ParticleTypes.PALE_OAK_LEAVES, ParticleTypes.TINTED_LEAVES,
		ParticleTypes.RED_POPLAR_LEAVES, ParticleTypes.ORANGE_POPLAR_LEAVES, ParticleTypes.YELLOW_POPLAR_LEAVES,
		ParticleTypes.DRIPPING_WATER, ParticleTypes.FALLING_WATER, ParticleTypes.DRIPPING_LAVA, ParticleTypes.FALLING_LAVA,
		ParticleTypes.LANDING_LAVA, ParticleTypes.DRIPPING_HONEY, ParticleTypes.FALLING_HONEY, ParticleTypes.LANDING_HONEY,
		ParticleTypes.FALLING_NECTAR, ParticleTypes.DRIPPING_OBSIDIAN_TEAR, ParticleTypes.FALLING_OBSIDIAN_TEAR,
		ParticleTypes.LANDING_OBSIDIAN_TEAR, ParticleTypes.DRIPPING_DRIPSTONE_LAVA, ParticleTypes.FALLING_DRIPSTONE_LAVA,
		ParticleTypes.DRIPPING_DRIPSTONE_WATER, ParticleTypes.FALLING_DRIPSTONE_WATER,
		ParticleTypes.BUBBLE_COLUMN_UP, ParticleTypes.CURRENT_DOWN,
	});

	private AmbientParticles() {
	}

	public static boolean shouldSkip(ParticleOptions options, double x, double y, double z) {
		WellOptiConfig.Particles cfg = WellOptiConfig.get().particles;
		if (!cfg.enabled || !WellOptiClient.active || !AMBIENT.contains(options.getType())) {
			return false;
		}

		if (cfg.ambientDensity < 100 && ThreadLocalRandom.current().nextInt(100) >= cfg.ambientDensity) {
			return true;
		}

		if (cfg.ambientDistance > 0) {
			Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
			double dx = x - cam.x;
			double dy = y - cam.y;
			double dz = z - cam.z;
			return dx * dx + dy * dy + dz * dz > (double) cfg.ambientDistance * cfg.ambientDistance;
		}
		return false;
	}
}
