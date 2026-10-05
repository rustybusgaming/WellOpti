package dev.wellopti.compat;

import dev.wellopti.WellOptiClient;
import dev.wellopti.hud.HudCanvas;
import dev.wellopti.hud.PerformanceHud;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import java.util.Optional;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Minecraft 26.2+: screens, chat and toasts live on {@code Minecraft.gui}. */
public final class Mc {
	private Mc() {
	}

	public static @Nullable Screen screen(Minecraft minecraft) {
		return minecraft.gui.screen();
	}

	public static void setScreen(Minecraft minecraft, @Nullable Screen screen) {
		minecraft.gui.setScreen(screen);
	}

	public static boolean isHudHidden(Minecraft minecraft) {
		return minecraft.gui.hud.isHidden();
	}

	public static ToastManager toasts(Minecraft minecraft) {
		return minecraft.gui.toastManager();
	}

	public static void systemMessage(Minecraft minecraft, Component message) {
		minecraft.gui.hud.getChat().addClientSystemMessage(message);
	}

	public static Vec3 cameraPosition(Minecraft minecraft) {
		return minecraft.gameRenderer.mainCamera().position();
	}

	public static boolean isWindowFocused(Minecraft minecraft) {
		return minecraft.getWindow().isFocused();
	}

	/** Shows a short message above the hotbar. */
	public static void actionBar(Minecraft minecraft, Component message) {
		if (minecraft.player != null) {
			minecraft.player.sendOverlayMessage(message);
		}
	}

	public static KeyMapping registerKey(KeyMapping key) {
		return KeyMappingHelper.registerKeyMapping(key);
	}

	public static void registerHud(String path, PerformanceHud hud) {
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(WellOptiClient.MOD_ID, path), (graphics, deltaTracker) -> hud.draw(new HudCanvas() {
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
				graphics.text(font, text, x, y, color, false);
			}
		}));
	}

	public static KeyMapping.Category keyCategory(String path) {
		return KeyMapping.Category.register(Identifier.fromNamespaceAndPath(WellOptiClient.MOD_ID, path));
	}

	/** A vanilla particle type by name, or empty if this Minecraft version doesn't have it. */
	public static Optional<ParticleType<?>> particleType(String path) {
		return BuiltInRegistries.PARTICLE_TYPE.getOptional(Identifier.withDefaultNamespace(path)).map(type -> (ParticleType<?>) type);
	}
	public static void addHeader(OptionsList list, Component text, Font font) {
		list.addHeader(text);
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
		return new OptionInstance<>(key, tooltip, label, new OptionInstance.IntRange(minStep, maxStep).xmap(step -> step * stepSize, value -> Math.round((float) value / stepSize), true), initial, setter::accept);
	}

	/** Re-applies volume to sounds already playing. */
	public static void refreshMasterVolume(Minecraft minecraft) {
		minecraft.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
	}
}
