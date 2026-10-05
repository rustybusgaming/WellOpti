package dev.wellopti.audio;

import dev.wellopti.compat.Mc;
import com.mojang.blaze3d.platform.Window;
import dev.wellopti.WellOptiClient;
import dev.wellopti.config.WellOptiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

/** Turns the game down while you're in another window, and back up when you return. */
public final class BackgroundAudio {
	private static volatile float multiplier = 1.0F;

	private BackgroundAudio() {
	}

	/** Applied on top of every sound's volume by {@code SoundEngineMixin}. */
	public static float multiplier() {
		return multiplier;
	}

	/** Called every client tick; only pokes the sound engine when the multiplier actually changes. */
	public static void tick(Minecraft minecraft) {
		float target = 1.0F;
		if (WellOptiClient.active) {
			WellOptiConfig.BackgroundAudio cfg = WellOptiConfig.get().backgroundAudio;
			Window window = minecraft.getWindow();
			if (window.isIconified()) {
				target = cfg.minimizedVolume / 100.0F;
			} else if (!Mc.isWindowFocused(minecraft)) {
				target = cfg.unfocusedVolume / 100.0F;
			}
		}

		if (target != multiplier) {
			multiplier = target;
			minecraft.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
		}
	}
}
