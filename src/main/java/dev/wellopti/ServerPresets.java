package dev.wellopti;

import dev.wellopti.compat.Mc;
import dev.wellopti.config.Preset;
import dev.wellopti.config.WellOptiConfig;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

/** Remembers which preset you picked on each server, and switches back to it when you join that server again. */
public final class ServerPresets {
	private ServerPresets() {
	}

	/** "singleplayer", "server:<address>", or empty when not in a world. */
	public static Optional<String> currentKey(Minecraft minecraft) {
		if (minecraft.level == null) {
			return Optional.empty();
		}
		if (minecraft.hasSingleplayerServer()) {
			return Optional.of("singleplayer");
		}
		ServerData server = minecraft.getCurrentServer();
		return server == null || server.ip == null ? Optional.empty() : Optional.of("server:" + server.ip.trim().toLowerCase(Locale.ROOT));
	}

	/** Called after the player picks a preset: remember it for wherever they are. */
	public static void remember(Minecraft minecraft, Preset preset) {
		WellOptiConfig cfg = WellOptiConfig.get();
		if (!cfg.perServerPresets) {
			return;
		}
		currentKey(minecraft).ifPresent(key -> cfg.serverPresets.put(key, preset.id()));
	}

	public static Optional<Preset> rememberedHere(Minecraft minecraft) {
		WellOptiConfig cfg = WellOptiConfig.get();
		return currentKey(minecraft).map(cfg.serverPresets::get).flatMap(Preset::byId);
	}

	private static boolean pendingRestore;

	/** Called from the join event, which can fire before the world is set; the next tick with a world does the work. */
	public static void joinedServer() {
		pendingRestore = true;
	}

	public static void tick(Minecraft minecraft) {
		if (pendingRestore && minecraft.level != null) {
			pendingRestore = false;
			restore(minecraft);
		}
	}

	/** Switch to the preset remembered for the current server, if there is one. */
	private static void restore(Minecraft minecraft) {
		WellOptiConfig cfg = WellOptiConfig.get();
		if (!cfg.perServerPresets) {
			return;
		}
		rememberedHere(minecraft).ifPresent(preset -> {
			preset.applyTo(cfg);
			cfg.save();
			Mc.actionBar(minecraft, Component.translatable("wellopti.preset.restored", Component.translatable(preset.translationKey()))
				.withStyle(ChatFormatting.GOLD));
		});
	}
}
