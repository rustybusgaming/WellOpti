package dev.wellopti;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.CommandDispatcher;
import dev.wellopti.config.WellOptiConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WellOptiClient implements ClientModInitializer {
	public static final String MOD_ID = "wellopti";
	public static final Logger LOGGER = LoggerFactory.getLogger("WellOpti");

	@Override
	public void onInitializeClient() {
		WellOptiConfig.load();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> registerCommands(dispatcher));
		LOGGER.info("WellOpti loaded. Minecraft is now running very goodly.");
	}

	private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal(MOD_ID)
			.executes(ctx -> {
				WellOptiConfig cfg = WellOptiConfig.get();
				FabricClientCommandSource source = ctx.getSource();
				source.sendFeedback(Component.literal("WellOpti").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
				source.sendFeedback(line("Dynamic FPS", cfg.dynamicFps.enabled,
					"unfocused " + cfg.dynamicFps.unfocusedFps + " fps, minimised " + cfg.dynamicFps.minimizedFps + " fps"));
				source.sendFeedback(line("Entity culling", cfg.entityCulling.enabled,
					"items " + cfg.entityCulling.droppedItems + ", xp " + cfg.entityCulling.experienceOrbs
						+ ", frames " + cfg.entityCulling.itemFrames + ", stands " + cfg.entityCulling.armorStands));
				source.sendFeedback(line("Block entity culling", cfg.blockEntityCulling.enabled,
					"signs " + cfg.blockEntityCulling.signText + ", banners " + cfg.blockEntityCulling.banners
						+ ", skulls " + cfg.blockEntityCulling.skulls + ", storage " + cfg.blockEntityCulling.storage
						+ ", displays " + cfg.blockEntityCulling.itemDisplays));
				source.sendFeedback(line("Particle cap", cfg.particles.enabled, cfg.particles.maxParticles + " max"));
				source.sendFeedback(Component.literal("Edit config/wellopti.json, then /wellopti reload").withStyle(ChatFormatting.GRAY));
				return 1;
			})
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

	private static Component line(String name, boolean enabled, String detail) {
		return Component.literal(name + ": ")
			.append(Component.literal(enabled ? "on" : "off").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED))
			.append(Component.literal(enabled ? " (" + detail + ")" : "").withStyle(ChatFormatting.GRAY));
	}
}
