package dev.wellopti.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.wellopti.gui.WellOptiConfigScreen;

/** Adds a config button to WellOpti's entry in Mod Menu. Only loaded when Mod Menu is installed. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return WellOptiConfigScreen::new;
	}
}
