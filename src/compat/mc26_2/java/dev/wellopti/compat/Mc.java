package dev.wellopti.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
}
