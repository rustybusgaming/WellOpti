package dev.wellopti.compat;

import dev.wellopti.WellOptiClient;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Optional Iris (shader) support, looked up by reflection so WellOpti doesn't need Iris to build or run.
 *
 * <p>Shaders draw a shadow pass from the sun's point of view, and Iris runs entities through the same
 * visibility check as the normal pass. Anything WellOpti decides from the camera's point of view (hidden
 * behind a wall, or one too many in a crowded block) must not apply there, or mobs would lose their shadows.
 */
public final class Shaders {
	private static final MethodHandle IS_RENDERING_SHADOW_PASS = lookup();

	private Shaders() {
	}

	public static boolean isRenderingShadowPass() {
		if (IS_RENDERING_SHADOW_PASS == null) {
			return false;
		}
		try {
			return (boolean) IS_RENDERING_SHADOW_PASS.invokeExact();
		} catch (Throwable t) {
			return false;
		}
	}

	private static MethodHandle lookup() {
		if (!FabricLoader.getInstance().isModLoaded("iris")) {
			return null;
		}
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			MethodHandle getInstance = lookup.findStatic(api, "getInstance", MethodType.methodType(api));
			MethodHandle isShadow = lookup.findVirtual(api, "isRenderingShadowPass", MethodType.methodType(boolean.class));
			// Bind to the singleton once, giving a ()boolean handle.
			return MethodHandles.foldArguments(isShadow, getInstance);
		} catch (ReflectiveOperationException | LinkageError e) {
			WellOptiClient.LOGGER.warn("Iris is installed but its API couldn't be found; shadow-pass handling is off", e);
			return null;
		}
	}
}
