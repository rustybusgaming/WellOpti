package dev.wellopti.gui;

import dev.wellopti.compat.Mc;
import com.mojang.serialization.Codec;
import dev.wellopti.WellOptiClient;
import dev.wellopti.bench.Benchmark;
import dev.wellopti.config.Preset;
import dev.wellopti.config.WellOptiConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** In-game settings, built from vanilla option widgets so it looks and behaves like the video settings screen. */
public class WellOptiConfigScreen extends OptionsSubScreen {
	public WellOptiConfigScreen(Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable("wellopti.options.title"));
	}

	@Override
	protected void addOptions() {
		WellOptiConfig cfg = WellOptiConfig.get();
		WellOptiConfig.DynamicFps fps = cfg.dynamicFps;
		WellOptiConfig.EntityCulling entities = cfg.entityCulling;
		WellOptiConfig.BlockEntityCulling blockEntities = cfg.blockEntityCulling;
		WellOptiConfig.Particles particles = cfg.particles;
		WellOptiConfig.OcclusionCulling occlusion = cfg.occlusionCulling;
		WellOptiConfig.BackgroundAudio audio = cfg.backgroundAudio;
		WellOptiConfig.Hud hud = cfg.hud;
		WellOptiConfig.Mobs mobs = cfg.mobs;
		WellOptiConfig.Adaptive adaptive = cfg.adaptive;

		Mc.addHeader(this.list, Component.translatable("wellopti.options.presets"), this.font);
		List<AbstractWidget> presetButtons = new ArrayList<>();
		for (Preset preset : Preset.values()) {
			presetButtons.add(Button.builder(Component.translatable(preset.translationKey()), button -> this.applyPreset(preset))
				.tooltip(Tooltip.create(Component.translatable(preset.translationKey() + ".tooltip")))
				.build());
		}
		this.list.addSmall(presetButtons);

		Button benchmark = Button.builder(Component.translatable("wellopti.options.benchmark"), button -> {
				this.onClose();
				Mc.setScreen(this.minecraft, null);
				Benchmark.start(this.minecraft);
			})
			.tooltip(Tooltip.create(Component.translatable(this.minecraft.level == null ? "wellopti.options.benchmark.needWorld" : "wellopti.options.benchmark.tooltip")))
			.build();
		benchmark.active = this.minecraft.level != null && !Benchmark.isRunning();
		this.list.addSmall(benchmark, null);

		Mc.addHeader(this.list, Component.translatable("wellopti.options.occlusionCulling"), this.font);
		this.list.addBig(toggle("wellopti.options.occlusionCulling.enabled", occlusion.enabled, v -> occlusion.enabled = v));
		this.list.addSmall(
			toggle("wellopti.options.occlusionCulling.entities", occlusion.entities, v -> occlusion.entities = v),
			toggle("wellopti.options.occlusionCulling.blockEntities", occlusion.blockEntities, v -> occlusion.blockEntities = v)
		);

		Mc.addHeader(this.list, Component.translatable("wellopti.options.dynamicFps"), this.font);
		this.list.addBig(toggle("wellopti.options.dynamicFps.enabled", fps.enabled, v -> fps.enabled = v));
		this.list.addSmall(
			fpsSlider("wellopti.options.dynamicFps.unfocused", 1, 60, fps.unfocusedFps, v -> fps.unfocusedFps = v),
			fpsSlider("wellopti.options.dynamicFps.minimized", 1, 30, fps.minimizedFps, v -> fps.minimizedFps = v)
		);
		this.list.addSmall(slider("wellopti.options.dynamicFps.paused", 0, 26, 10, fps.pausedFps, v -> fps.pausedFps = v, WellOptiConfigScreen::fpsLabel));

		Mc.addHeader(this.list, Component.translatable("wellopti.options.entityCulling"), this.font);
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
			distance("wellopti.options.entityCulling.ambientMobs", entities.ambientMobs, v -> entities.ambientMobs = v),
			distance("wellopti.options.entityCulling.passiveMobs", entities.passiveMobs, v -> entities.passiveMobs = v),
			distance("wellopti.options.entityCulling.villagers", entities.villagers, v -> entities.villagers = v),
			distance("wellopti.options.entityCulling.hostileMobs", entities.hostileMobs, v -> entities.hostileMobs = v),
			distance("wellopti.options.entityCulling.paintings", entities.paintings, v -> entities.paintings = v),
			distance("wellopti.options.entityCulling.vehicles", entities.vehicles, v -> entities.vehicles = v)
		);

		Mc.addHeader(this.list, Component.translatable("wellopti.options.mobs"), this.font);
		this.list.addSmall(
			distance("wellopti.options.mobs.equipmentDistance", mobs.equipmentDistance, v -> mobs.equipmentDistance = v),
			slider("wellopti.options.mobs.nameTagDistance", 0, 16, 4, mobs.nameTagDistance, v -> mobs.nameTagDistance = v, WellOptiConfigScreen::blocksLabel),
			slider("wellopti.options.mobs.crowdLimit", 0, 32, 1, mobs.crowdLimit, v -> mobs.crowdLimit = v, (caption, v) -> v == 0
				? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF)
				: Options.genericValueLabel(caption, Component.translatable("wellopti.options.perBlock", v)))
		);

		Mc.addHeader(this.list, Component.translatable("wellopti.options.blockEntityCulling"), this.font);
		this.list.addSmall(
			toggle("wellopti.options.blockEntityCulling.enabled", blockEntities.enabled, v -> blockEntities.enabled = v),
			toggle("wellopti.options.disableWhileScoping", blockEntities.disableWhileScoping, v -> blockEntities.disableWhileScoping = v)
		);
		this.list.addSmall(
			distance("wellopti.options.blockEntityCulling.signText", blockEntities.signText, v -> blockEntities.signText = v),
			distance("wellopti.options.blockEntityCulling.banners", blockEntities.banners, v -> blockEntities.banners = v),
			distance("wellopti.options.blockEntityCulling.skulls", blockEntities.skulls, v -> blockEntities.skulls = v),
			distance("wellopti.options.blockEntityCulling.storage", blockEntities.storage, v -> blockEntities.storage = v),
			distance("wellopti.options.blockEntityCulling.itemDisplays", blockEntities.itemDisplays, v -> blockEntities.itemDisplays = v),
			distance("wellopti.options.blockEntityCulling.spawners", blockEntities.spawners, v -> blockEntities.spawners = v)
		);

		Mc.addHeader(this.list, Component.translatable("wellopti.options.particles"), this.font);
		this.list.addSmall(
			toggle("wellopti.options.particles.enabled", particles.enabled, v -> particles.enabled = v),
			slider("wellopti.options.particles.max", 0, 40, 500, particles.maxParticles, v -> particles.maxParticles = v,
				(caption, v) -> v == 0 ? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF) : Options.genericValueLabel(caption, v)),
			distance("wellopti.options.particles.ambientDistance", particles.ambientDistance, v -> particles.ambientDistance = v),
			slider("wellopti.options.particles.ambientDensity", 0, 20, 5, particles.ambientDensity, v -> particles.ambientDensity = v,
				(caption, v) -> Options.genericValueLabel(caption, Component.translatable("wellopti.options.percent", v)))
		);

		Mc.addHeader(this.list, Component.translatable("wellopti.options.adaptive"), this.font);
		this.list.addBig(toggle("wellopti.options.adaptive.enabled", adaptive.enabled, v -> adaptive.enabled = v));
		this.list.addSmall(
			slider("wellopti.options.adaptive.targetFps", 1, 26, 10, adaptive.targetFps, v -> adaptive.targetFps = v, WellOptiConfigScreen::fpsLabel),
			slider("wellopti.options.adaptive.minScale", 2, 20, 5, adaptive.minScale, v -> adaptive.minScale = v,
				(caption, v) -> Options.genericValueLabel(caption, Component.translatable("wellopti.options.percent", v)))
		);

		Mc.addHeader(this.list, Component.translatable("wellopti.options.backgroundAudio"), this.font);
		this.list.addSmall(
			volume("wellopti.options.backgroundAudio.unfocused", audio.unfocusedVolume, v -> audio.unfocusedVolume = v),
			volume("wellopti.options.backgroundAudio.minimized", audio.minimizedVolume, v -> audio.minimizedVolume = v)
		);

		Mc.addHeader(this.list, Component.translatable("wellopti.options.hud"), this.font);
		this.list.addSmall(
			toggle("wellopti.options.hud.enabled", hud.enabled, v -> hud.enabled = v),
			new OptionInstance<>(
				"wellopti.options.hud.corner",
				tooltip("wellopti.options.hud.corner"),
				(caption, corner) -> Options.genericValueLabel(caption, Component.translatable("wellopti.options.hud.corner." + corner.name().toLowerCase(java.util.Locale.ROOT))),
				new OptionInstance.Enum<>(List.of(WellOptiConfig.HudCorner.values()), Codec.INT.xmap(i -> WellOptiConfig.HudCorner.values()[i], Enum::ordinal)),
				hud.corner,
				v -> hud.corner = v
			),
			toggle("wellopti.options.hud.showMemory", hud.showMemory, v -> hud.showMemory = v),
			toggle("wellopti.options.hud.showCulling", hud.showCulling, v -> hud.showCulling = v)
		);

		Mc.addHeader(this.list, Component.translatable("wellopti.options.misc"), this.font);
		this.list.addSmall(toggle("wellopti.options.memoryAdvisor", cfg.memoryAdvisor, v -> cfg.memoryAdvisor = v));
	}

	/** Applies a preset, then rebuilds the screen so every slider shows its new value. */
	private void applyPreset(Preset preset) {
		WellOptiClient.applyPreset(preset);
		Mc.setScreen(this.minecraft, new WellOptiConfigScreen(this.lastScreen));
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
		return slider(key, 0, 32, 4, initial, setter, WellOptiConfigScreen::blocksLabel);
	}

	private static Component blocksLabel(Component caption, int value) {
		return value == 0
			? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF)
			: Options.genericValueLabel(caption, Component.translatable("wellopti.options.blocks", value));
	}

	private static OptionInstance<Integer> volume(String key, int initial, IntConsumer setter) {
		return slider(key, 0, 20, 5, initial, setter, (caption, v) -> Options.genericValueLabel(caption, Component.translatable("wellopti.options.percent", v)));
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
		return Mc.steppedSlider(key, tooltip(key), label, minStep, maxStep, stepSize, initial, setter);
	}

	private static <T> OptionInstance.TooltipSupplier<T> tooltip(String key) {
		return OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip"));
	}

	@FunctionalInterface
	private interface BooleanSetter {
		void set(boolean value);
	}
}
