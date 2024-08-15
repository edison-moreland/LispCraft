package net.devdude.lispcraft.mod.client;

import net.devdude.lispcraft.runtime.ConsoleRuntime;
import net.fabricmc.api.ClientModInitializer;

public class ModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        System.out.println(ConsoleRuntime.HELLO);
    }
}
