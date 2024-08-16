package net.devdude.lispcraft.mod.client;

import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.mod.client.screens.ConsoleScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class ModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(Mod.ScreenHandlers.CONSOLE, ConsoleScreen::new);
    }
}
