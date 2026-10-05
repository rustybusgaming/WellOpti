package dev.wellopti.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Minecraft 26.1.x: screens, chat and toasts live directly on {@code Minecraft}. */
public final class Mc {
	private Mc() {
	}

	public static @Nullable Screen screen(Minecraft minecraft) {
		return minecraft.screen;
	}

	public static void setScreen(Minecraft minecraft, @Nullable Screen screen) {
		minecraft.setScreen(screen);
	}

	public static boolean isHudHidden(Minecraft minecraft) {
		return minecraft.options.hideGui;
	}

	public static ToastManager toasts(Minecraft minecraft) {
		return minecraft.getToastManager();
	}

	public static void systemMessage(Minecraft minecraft, Component message) {
		minecraft.gui.getChat().addClientSystemMessage(message);
	}

	public static Vec3 cameraPosition(Minecraft minecraft) {
		return minecraft.gameRenderer.getMainCamera().position();
	}
}
