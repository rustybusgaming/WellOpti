package dev.wellopti.compat;

import dev.wellopti.WellOptiClient;
import dev.wellopti.hud.HudCanvas;
import dev.wellopti.hud.PerformanceHud;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.StringWidget;
import java.util.Optional;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Minecraft 1.21.9-1.21.10: like 1.21.11, but identifiers are still called ResourceLocation. */
public final class Mc {
	private Mc() {
	}

	public static Screen screen(Minecraft minecraft) {
		return minecraft.screen;
	}

	public static void setScreen(Minecraft minecraft, Screen screen) {
		minecraft.setScreen(screen);
	}

	public static boolean isHudHidden(Minecraft minecraft) {
		return minecraft.options.hideGui;
	}

	public static ToastManager toasts(Minecraft minecraft) {
		return minecraft.getToastManager();
	}

	public static void systemMessage(Minecraft minecraft, Component message) {
		minecraft.gui.getChat().addMessage(message);
	}

	public static Vec3 cameraPosition(Minecraft minecraft) {
		return minecraft.gameRenderer.getMainCamera().position();
	}

	public static boolean isWindowFocused(Minecraft minecraft) {
		return minecraft.isWindowActive();
	}

	/** Shows a short message above the hotbar. */
	public static void actionBar(Minecraft minecraft, Component message) {
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(message, true);
		}
	}

	public static KeyMapping registerKey(KeyMapping key) {
		return KeyBindingHelper.registerKeyBinding(key);
	}

	public static void registerHud(String path, PerformanceHud hud) {
		HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath(WellOptiClient.MOD_ID, path), (graphics, deltaTracker) -> hud.draw(new HudCanvas() {
			@Override
			public int width() {
				return graphics.guiWidth();
			}

			@Override
			public int height() {
				return graphics.guiHeight();
			}

			@Override
			public void fill(int x0, int y0, int x1, int y1, int color) {
				graphics.fill(x0, y0, x1, y1, color);
			}

			@Override
			public void text(Font font, String text, int x, int y, int color) {
				graphics.drawString(font, text, x, y, color, false);
			}
		}));
	}

	public static KeyMapping.Category keyCategory(String path) {
		return KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(WellOptiClient.MOD_ID, path));
	}

	/** A vanilla particle type by name, or empty if this Minecraft version doesn't have it. */
	public static Optional<ParticleType<?>> particleType(String path) {
		return BuiltInRegistries.PARTICLE_TYPE.getOptional(ResourceLocation.withDefaultNamespace(path)).map(type -> (ParticleType<?>) type);
	}
	/** This version's options list has no section headers, so a plain text widget stands in for one. */
	public static void addHeader(OptionsList list, Component text, Font font) {
		list.addSmall(new StringWidget(150, 20, text.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), font), null);
	}

	/** A slider option over minStep..maxStep whose values are multiples of stepSize (so 0..32 by 4 gives 0..128). */
	public static OptionInstance<Integer> steppedSlider(
		String key,
		OptionInstance.TooltipSupplier<Integer> tooltip,
		OptionInstance.CaptionBasedToString<Integer> label,
		int minStep,
		int maxStep,
		int stepSize,
		int initial,
		IntConsumer setter
	) {
		return new OptionInstance<>(key, tooltip, label, new OptionInstance.IntRange(minStep, maxStep).xmap(step -> step * stepSize, value -> Math.round((float) value / stepSize)), initial, setter::accept);
	}

	/** Re-applies volume to sounds already playing. */
	public static void refreshMasterVolume(Minecraft minecraft) {
		minecraft.getSoundManager().updateSourceVolume(SoundSource.MASTER);
	}
}
