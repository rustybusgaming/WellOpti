package dev.wellopti.mixin;

import dev.wellopti.WellOptiClient;
import dev.wellopti.WellOptiStats;
import dev.wellopti.config.WellOptiConfig;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops explosions, farms and particle-spam plugins from flooding the particle engine. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
	@Shadow
	@Final
	private Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void wellopti$capParticles(Particle particle, CallbackInfo ci) {
		WellOptiConfig.Particles cfg = WellOptiConfig.get().particles;
		if (!cfg.enabled || !WellOptiClient.active || cfg.maxParticles <= 0) {
			return;
		}

		// Item pickup animations and the elder guardian jumpscare carry gameplay meaning; always let them through.
		ParticleRenderType group = particle.getGroup();
		if (group == ParticleRenderType.ITEM_PICKUP || group == ParticleRenderType.ELDER_GUARDIANS) {
			return;
		}

		int total = this.particlesToAdd.size();
		for (ParticleGroup<?> existing : this.particles.values()) {
			total += existing.size();
		}

		if (total >= cfg.maxParticles) {
			WellOptiStats.droppedParticles++;
			ci.cancel();
		}
	}
}
