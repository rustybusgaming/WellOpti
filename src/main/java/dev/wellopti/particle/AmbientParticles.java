package dev.wellopti.particle;

import dev.wellopti.WellOptiClient;
import dev.wellopti.compat.Mc;
import dev.wellopti.config.WellOptiConfig;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.phys.Vec3;

/**
 * Thins out purely decorative particles that blocks and biomes spawn constantly. Particles that tell you
 * something (hits, potion effects, explosions, block breaking, portals, redstone) are never touched.
 */
public final class AmbientParticles {
	/**
	 * Looked up by name rather than referenced directly, so particles that only exist in newer Minecraft
	 * versions (the 26.3 poplar leaves) are simply skipped on older ones.
	 */
	private static final String[] AMBIENT_IDS = {
		"campfire_cosy_smoke", "campfire_signal_smoke",
		"rain", "underwater", "mycelium",
		"ash", "white_ash", "crimson_spore", "warped_spore",
		"spore_blossom_air", "falling_spore_blossom", "firefly",
		"cherry_leaves", "pale_oak_leaves", "tinted_leaves",
		"red_poplar_leaves", "orange_poplar_leaves", "yellow_poplar_leaves",
		"dripping_water", "falling_water", "dripping_lava", "falling_lava", "landing_lava",
		"dripping_honey", "falling_honey", "landing_honey", "falling_nectar",
		"dripping_obsidian_tear", "falling_obsidian_tear", "landing_obsidian_tear",
		"dripping_dripstone_lava", "falling_dripstone_lava", "dripping_dripstone_water", "falling_dripstone_water",
		"bubble_column_up", "current_down",
	};

	private static Set<ParticleType<?>> ambient;

	private AmbientParticles() {
	}

	public static boolean shouldSkip(ParticleOptions options, double x, double y, double z) {
		WellOptiConfig.Particles cfg = WellOptiConfig.get().particles;
		if (!cfg.enabled || !WellOptiClient.active || !ambient().contains(options.getType())) {
			return false;
		}

		if (cfg.ambientDensity < 100 && ThreadLocalRandom.current().nextInt(100) >= cfg.ambientDensity) {
			return true;
		}

		if (cfg.ambientDistance > 0) {
			Vec3 cam = Mc.cameraPosition(Minecraft.getInstance());
			double dx = x - cam.x;
			double dy = y - cam.y;
			double dz = z - cam.z;
			return dx * dx + dy * dy + dz * dz > (double) cfg.ambientDistance * cfg.ambientDistance;
		}
		return false;
	}

	private static Set<ParticleType<?>> ambient() {
		if (ambient == null) {
			Set<ParticleType<?>> types = new ReferenceOpenHashSet<>();
			for (String id : AMBIENT_IDS) {
				Mc.particleType(id).ifPresent(types::add);
			}
			ambient = types;
		}
		return ambient;
	}
}
