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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.wellopti.particle.AmbientParticles;
import net.minecraft.core.particles.ParticleOptions;

/** Stops explosions, farms and particle-spam plugins from flooding the particle engine. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
	@Shadow
	@Final
	private Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	/** Ambient particles go through here (not straight to add), so this is where we can see their type and position. */
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void wellopti$thinAmbientParticles(
		ParticleOptions options, double x, double y, double z, double xa, double ya, double za, CallbackInfoReturnable<Particle> cir
	) {
		if (AmbientParticles.shouldSkip(options, x, y, z)) {
			WellOptiStats.droppedParticles++;
			cir.setReturnValue(null);
		}
	}

	/**
	 * Live particle count, refreshed once per tick and bumped as particles are added in between. Counting by
	 * walking every particle group on each add would create an iterator per particle spawned.
	 */
	@Unique
	private int wellopti$liveCount;

	@Inject(method = "tick", at = @At("TAIL"))
	private void wellopti$recount(CallbackInfo ci) {
		int total = this.particlesToAdd.size();
		for (ParticleGroup<?> group : this.particles.values()) {
			total += group.size();
		}
		this.wellopti$liveCount = total;
	}

	@Inject(method = "clearParticles", at = @At("TAIL"), require = 0)
	private void wellopti$resetCount(CallbackInfo ci) {
		this.wellopti$liveCount = 0;
	}

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

		if (this.wellopti$liveCount >= cfg.maxParticles) {
			WellOptiStats.droppedParticles++;
			ci.cancel();
			return;
		}
		this.wellopti$liveCount++;
	}
}
