package dev.wellopti;

import dev.wellopti.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.wellopti.audio.BackgroundAudio;
import dev.wellopti.bench.Benchmark;
import dev.wellopti.config.Preset;
import dev.wellopti.config.WellOptiConfig;
import dev.wellopti.gui.WellOptiConfigScreen;
import dev.wellopti.hud.PerformanceHud;
import dev.wellopti.occlusion.OcclusionCuller;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WellOptiClient implements ClientModInitializer {
	public static final String MOD_ID = "wellopti";
	public static final Logger LOGGER = LoggerFactory.getLogger("WellOpti");

	/**
	 * Master switch flipped by the toggle key, so you can compare FPS with and without WellOpti
	 * without touching the config. Not saved: every launch starts with WellOpti on.
	 */
	public static boolean active = true;

	private static final KeyMapping.Category KEY_CATEGORY = Mc.keyCategory(MOD_ID);
	private static KeyMapping toggleKey;
	private static KeyMapping configKey;
	private static KeyMapping hudKey;

	/** Commands run while the chat screen is open, which closes right after; open our screen a tick later instead. */
	private static boolean openConfigNextTick;

	@Override
	public void onInitializeClient() {
		WellOptiConfig.load();

		toggleKey = Mc.registerKey(new KeyMapping("key.wellopti.toggle", InputConstants.KEY_F7, KEY_CATEGORY));
		configKey = Mc.registerKey(new KeyMapping("key.wellopti.config", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));
		hudKey = Mc.registerKey(new KeyMapping("key.wellopti.hud", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));

		Mc.registerHud("performance_hud", new PerformanceHud());
		ClientTickEvents.END_CLIENT_TICK.register(WellOptiClient::onEndTick);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, minecraft) -> {
			MemoryAdvisor.onJoin(minecraft);
			ServerPresets.joinedServer();
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> registerCommands(dispatcher));
		LOGGER.info("WellOpti loaded. Minecraft is now running very goodly.");
	}

	private static void onEndTick(Minecraft minecraft) {
		OcclusionCuller.tick(minecraft.level);
		AdaptiveDistance.tick(minecraft);
		WellOptiStats.tick();
		BackgroundAudio.tick(minecraft);
		Benchmark.tick(minecraft);
		ServerPresets.tick(minecraft);
		MemoryAdvisor.tick(minecraft);

		while (hudKey.consumeClick()) {
			toggleHud(minecraft);
		}

		while (toggleKey.consumeClick()) {
			toggle(minecraft);
		}

		while (configKey.consumeClick()) {
			openConfigNextTick = true;
		}

		if (openConfigNextTick) {
			openConfigNextTick = false;
			Mc.setScreen(minecraft, new WellOptiConfigScreen(Mc.screen(minecraft)));
		}
	}

	private static void toggle(Minecraft minecraft) {
		if (Benchmark.isRunning()) {
			Mc.actionBar(minecraft, Component.translatable("wellopti.benchmark.noToggle").withStyle(ChatFormatting.RED));
			return;
		}
		active = !active;
		Mc.actionBar(minecraft, Component.translatable(active ? "wellopti.toggle.on" : "wellopti.toggle.off")
			.withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
	}

	private static void toggleHud(Minecraft minecraft) {
		WellOptiConfig cfg = WellOptiConfig.get();
		cfg.hud.enabled = !cfg.hud.enabled;
		cfg.save();
	}

	private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal(MOD_ID)
			.executes(ctx -> {
				WellOptiConfig cfg = WellOptiConfig.get();
				FabricClientCommandSource source = ctx.getSource();
				source.sendFeedback(Component.literal("WellOpti").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
					.append(Component.literal(active ? "" : " (paused with the toggle key)").withStyle(ChatFormatting.RED)));
				source.sendFeedback(line("Dynamic FPS", cfg.dynamicFps.enabled,
					"unfocused " + cfg.dynamicFps.unfocusedFps + ", minimised " + cfg.dynamicFps.minimizedFps
						+ ", paused " + offIfZero(cfg.dynamicFps.pausedFps) + " fps"));
				source.sendFeedback(line("Entity culling", cfg.entityCulling.enabled,
					"items " + cfg.entityCulling.droppedItems + ", xp " + cfg.entityCulling.experienceOrbs
						+ ", frames " + cfg.entityCulling.itemFrames + ", stands " + cfg.entityCulling.armorStands
						+ ", arrows " + cfg.entityCulling.stuckArrows + ", ambient " + cfg.entityCulling.ambientMobs
						+ ", passive " + cfg.entityCulling.passiveMobs + ", villagers " + cfg.entityCulling.villagers
						+ ", hostile " + cfg.entityCulling.hostileMobs));
				source.sendFeedback(line("Block entity culling", cfg.blockEntityCulling.enabled,
					"signs " + cfg.blockEntityCulling.signText + ", banners " + cfg.blockEntityCulling.banners
						+ ", skulls " + cfg.blockEntityCulling.skulls + ", storage " + cfg.blockEntityCulling.storage
						+ ", displays " + cfg.blockEntityCulling.itemDisplays + ", spawners " + cfg.blockEntityCulling.spawners));
				source.sendFeedback(Component.literal("Mob detail: equipment " + offIfZero(cfg.mobs.equipmentDistance)
					+ ", name tags " + offIfZero(cfg.mobs.nameTagDistance) + ", crowd limit " + offIfZero(cfg.mobs.crowdLimit) + " per block")
					.withStyle(ChatFormatting.GRAY));
				source.sendFeedback(line("Adaptive", cfg.adaptive.enabled, "target " + cfg.adaptive.targetFps + " fps, now at "
					+ Math.round(AdaptiveDistance.scale() * 100) + "% distance"));
				source.sendFeedback(line("Particle cap", cfg.particles.enabled, offIfZero(cfg.particles.maxParticles) + " max"));
				source.sendFeedback(line("Occlusion culling", cfg.occlusionCulling.enabled,
					(cfg.occlusionCulling.entities ? "entities" : "") + (cfg.occlusionCulling.entities && cfg.occlusionCulling.blockEntities ? " + " : "")
						+ (cfg.occlusionCulling.blockEntities ? "block entities" : "")));
				source.sendFeedback(Component.literal("Background volume: unfocused " + cfg.backgroundAudio.unfocusedVolume
					+ "%, minimised " + cfg.backgroundAudio.minimizedVolume + "%").withStyle(ChatFormatting.GRAY));
				ServerPresets.rememberedHere(source.getClient()).ifPresent(preset -> source.sendFeedback(
					Component.literal("Preset remembered for this server: ").append(Component.translatable(preset.translationKey()))
						.withStyle(ChatFormatting.GRAY)));
				source.sendFeedback(Component.literal("/wellopti config to change settings").withStyle(ChatFormatting.GRAY));
				return 1;
			})
			.then(literal("config").executes(ctx -> {
				openConfigNextTick = true;
				return 1;
			}))
			.then(literal("benchmark")
				.executes(ctx -> {
					Benchmark.start(ctx.getSource().getClient());
					return 1;
				})
				.then(literal("stop").executes(ctx -> {
					Benchmark.stop(ctx.getSource().getClient(), Component.translatable("wellopti.benchmark.cancelled.manual"));
					return 1;
				})))
			.then(literal("hud").executes(ctx -> {
				toggleHud(ctx.getSource().getClient());
				ctx.getSource().sendFeedback(Component.literal("Performance HUD " + (WellOptiConfig.get().hud.enabled ? "shown." : "hidden.")));
				return 1;
			}))
			.then(literal("toggle").executes(ctx -> {
				toggle(ctx.getSource().getClient());
				return 1;
			}))
			.then(literal("reload").executes(ctx -> {
				WellOptiConfig.load();
				ctx.getSource().sendFeedback(Component.literal("WellOpti config reloaded.").withStyle(ChatFormatting.GREEN));
				return 1;
			}))
			.then(literal("stats").executes(ctx -> {
				double[] s = WellOptiStats.sinceLastCommand();
				ctx.getSource().sendFeedback(Component.literal(String.format(
					"Over the last %.0fs WellOpti skipped %,.0f far entity draws, %,.0f far block entity draws, "
						+ "%,.0f draws of things hidden behind walls, %,.0f draws of crowded mobs, %,.0f particles, "
						+ "and %,.0f client ticks of hidden entities.",
					s[6], s[0], s[1], s[2], s[4], s[3], s[5])));
				return 1;
			})));
	}

	/** Used by the preset buttons on the settings screen. */
	public static void applyPreset(Preset preset) {
		WellOptiConfig cfg = WellOptiConfig.get();
		preset.applyTo(cfg);
		ServerPresets.remember(Minecraft.getInstance(), preset);
		cfg.save();
	}

	/** Brigadier's own builder, which is the same on every Minecraft version (Fabric's helper was renamed in 26.1). */
	private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
		return LiteralArgumentBuilder.literal(name);
	}

	private static String offIfZero(int value) {
		return value == 0 ? "off" : Integer.toString(value);
	}

	private static Component line(String name, boolean enabled, String detail) {
		return Component.literal(name + ": ")
			.append(Component.literal(enabled ? "on" : "off").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED))
			.append(Component.literal(enabled ? " (" + detail + ")" : "").withStyle(ChatFormatting.GRAY));
	}
}
