package net.devdude.lispcraft.mod.client;

import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.mod.client.screens.ConsoleScreen;
import net.devdude.lispcraft.runtime.EvalTest;
import net.devdude.lispcraft.runtime.Printer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class ModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EvalTest.hello(new ModPrinter());
        HandledScreens.register(Mod.ScreenHandlers.CONSOLE, ConsoleScreen::new);
    }

    private static class ModPrinter implements Printer {

        @Override
        public void print(String string) {
            System.out.println(string);
        }
    }
}
