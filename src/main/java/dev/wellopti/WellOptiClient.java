package dev.wellopti;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import dev.wellopti.config.WellOptiConfig;
import dev.wellopti.gui.WellOptiConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID));
	private static KeyMapping toggleKey;
	private static KeyMapping configKey;

	/** Commands run while the chat screen is open, which closes right after; open our screen a tick later instead. */
	private static boolean openConfigNextTick;

	@Override
	public void onInitializeClient() {
		WellOptiConfig.load();

		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wellopti.toggle", InputConstants.KEY_F7, KEY_CATEGORY));
		configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wellopti.config", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(WellOptiClient::onEndTick);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> registerCommands(dispatcher));
		LOGGER.info("WellOpti loaded. Minecraft is now running very goodly.");
	}

	private static void onEndTick(Minecraft minecraft) {
		while (toggleKey.consumeClick()) {
			toggle(minecraft);
		}

		while (configKey.consumeClick()) {
			openConfigNextTick = true;
		}

		if (openConfigNextTick) {
			openConfigNextTick = false;
			minecraft.gui.setScreen(new WellOptiConfigScreen(minecraft.gui.screen()));
		}
	}

	private static void toggle(Minecraft minecraft) {
		active = !active;
		if (minecraft.player != null) {
			minecraft.player.sendOverlayMessage(Component.translatable(active ? "wellopti.toggle.on" : "wellopti.toggle.off")
				.withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
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
						+ ", arrows " + cfg.entityCulling.stuckArrows + ", ambient " + cfg.entityCulling.ambientMobs));
				source.sendFeedback(line("Block entity culling", cfg.blockEntityCulling.enabled,
					"signs " + cfg.blockEntityCulling.signText + ", banners " + cfg.blockEntityCulling.banners
						+ ", skulls " + cfg.blockEntityCulling.skulls + ", storage " + cfg.blockEntityCulling.storage
						+ ", displays " + cfg.blockEntityCulling.itemDisplays));
				source.sendFeedback(line("Particle cap", cfg.particles.enabled, offIfZero(cfg.particles.maxParticles) + " max"));
				source.sendFeedback(Component.literal("/wellopti config to change settings").withStyle(ChatFormatting.GRAY));
				return 1;
			})
			.then(literal("config").executes(ctx -> {
				openConfigNextTick = true;
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
				double seconds = WellOptiStats.secondsSinceReset();
				ctx.getSource().sendFeedback(Component.literal(String.format(
					"Over the last %.0fs WellOpti skipped %,d entity draws, %,d block entity draws and %,d particles (%.0f draws/s).",
					seconds, WellOptiStats.culledEntities, WellOptiStats.culledBlockEntities, WellOptiStats.droppedParticles,
					(WellOptiStats.culledEntities + WellOptiStats.culledBlockEntities) / seconds)));
				WellOptiStats.reset();
				return 1;
			})));
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
