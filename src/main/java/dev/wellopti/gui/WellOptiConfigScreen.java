package dev.wellopti.gui;

import dev.wellopti.config.WellOptiConfig;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** In-game settings, built from vanilla option widgets so it looks and behaves like the video settings screen. */
public class WellOptiConfigScreen extends OptionsSubScreen {
	public WellOptiConfigScreen(@Nullable Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable("wellopti.options.title"));
	}

	@Override
	protected void addOptions() {
		WellOptiConfig cfg = WellOptiConfig.get();
		WellOptiConfig.DynamicFps fps = cfg.dynamicFps;
		WellOptiConfig.EntityCulling entities = cfg.entityCulling;
		WellOptiConfig.BlockEntityCulling blockEntities = cfg.blockEntityCulling;
		WellOptiConfig.Particles particles = cfg.particles;

		this.list.addHeader(Component.translatable("wellopti.options.dynamicFps"));
		this.list.addBig(toggle("wellopti.options.dynamicFps.enabled", fps.enabled, v -> fps.enabled = v));
		this.list.addSmall(
			fpsSlider("wellopti.options.dynamicFps.unfocused", 1, 60, fps.unfocusedFps, v -> fps.unfocusedFps = v),
			fpsSlider("wellopti.options.dynamicFps.minimized", 1, 30, fps.minimizedFps, v -> fps.minimizedFps = v)
		);
		this.list.addSmall(slider("wellopti.options.dynamicFps.paused", 0, 26, 10, fps.pausedFps, v -> fps.pausedFps = v, WellOptiConfigScreen::fpsLabel));

		this.list.addHeader(Component.translatable("wellopti.options.entityCulling"));
		this.list.addSmall(
			toggle("wellopti.options.entityCulling.enabled", entities.enabled, v -> entities.enabled = v),
			toggle("wellopti.options.disableWhileScoping", entities.disableWhileScoping, v -> entities.disableWhileScoping = v)
		);
		this.list.addSmall(
			distance("wellopti.options.entityCulling.droppedItems", entities.droppedItems, v -> entities.droppedItems = v),
			distance("wellopti.options.entityCulling.experienceOrbs", entities.experienceOrbs, v -> entities.experienceOrbs = v),
			distance("wellopti.options.entityCulling.itemFrames", entities.itemFrames, v -> entities.itemFrames = v),
			distance("wellopti.options.entityCulling.armorStands", entities.armorStands, v -> entities.armorStands = v),
			distance("wellopti.options.entityCulling.stuckArrows", entities.stuckArrows, v -> entities.stuckArrows = v),
			distance("wellopti.options.entityCulling.ambientMobs", entities.ambientMobs, v -> entities.ambientMobs = v)
		);

		this.list.addHeader(Component.translatable("wellopti.options.blockEntityCulling"));
		this.list.addSmall(
			toggle("wellopti.options.blockEntityCulling.enabled", blockEntities.enabled, v -> blockEntities.enabled = v),
			toggle("wellopti.options.disableWhileScoping", blockEntities.disableWhileScoping, v -> blockEntities.disableWhileScoping = v)
		);
		this.list.addSmall(
			distance("wellopti.options.blockEntityCulling.signText", blockEntities.signText, v -> blockEntities.signText = v),
			distance("wellopti.options.blockEntityCulling.banners", blockEntities.banners, v -> blockEntities.banners = v),
			distance("wellopti.options.blockEntityCulling.skulls", blockEntities.skulls, v -> blockEntities.skulls = v),
			distance("wellopti.options.blockEntityCulling.storage", blockEntities.storage, v -> blockEntities.storage = v),
			distance("wellopti.options.blockEntityCulling.itemDisplays", blockEntities.itemDisplays, v -> blockEntities.itemDisplays = v)
		);

		this.list.addHeader(Component.translatable("wellopti.options.particles"));
		this.list.addSmall(
			toggle("wellopti.options.particles.enabled", particles.enabled, v -> particles.enabled = v),
			slider("wellopti.options.particles.max", 0, 40, 500, particles.maxParticles, v -> particles.maxParticles = v,
				(caption, v) -> v == 0 ? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF) : Options.genericValueLabel(caption, v))
		);
	}

	@Override
	public void removed() {
		super.removed();
		WellOptiConfig.get().save();
	}

	private static OptionInstance<Boolean> toggle(String key, boolean initial, BooleanSetter setter) {
		return OptionInstance.createBoolean(key, tooltip(key), initial, setter::set);
	}

	/** Distance slider from 0 to 128 blocks in steps of 4, where 0 means "never cull". */
	private static OptionInstance<Integer> distance(String key, int initial, IntConsumer setter) {
		return slider(key, 0, 32, 4, initial, setter, (caption, v) -> v == 0
			? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF)
			: Options.genericValueLabel(caption, Component.translatable("wellopti.options.blocks", v)));
	}

	private static OptionInstance<Integer> fpsSlider(String key, int min, int max, int initial, IntConsumer setter) {
		return slider(key, min, max, 1, initial, setter, WellOptiConfigScreen::fpsLabel);
	}

	private static Component fpsLabel(Component caption, int value) {
		return value == 0
			? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF)
			: Options.genericValueLabel(caption, Component.translatable("options.framerate", value));
	}

	private static OptionInstance<Integer> slider(
		String key, int minStep, int maxStep, int stepSize, int initial, IntConsumer setter, OptionInstance.CaptionBasedToString<Integer> label
	) {
		IntUnaryOperator toValue = step -> step * stepSize;
		IntUnaryOperator toStep = value -> Math.round((float) value / stepSize);
		return new OptionInstance<>(
			key,
			tooltip(key),
			label,
			new OptionInstance.IntRange(minStep, maxStep).xmap(toValue::applyAsInt, toStep::applyAsInt, true),
			initial,
			setter::accept
		);
	}

	private static <T> OptionInstance.TooltipSupplier<T> tooltip(String key) {
		return OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip"));
	}

	@FunctionalInterface
	private interface BooleanSetter {
		void set(boolean value);
	}
}
