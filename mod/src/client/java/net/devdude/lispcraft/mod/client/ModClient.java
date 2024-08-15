package net.devdude.lispcraft.mod.client;

import net.devdude.lispcraft.runtime.EvalTest;
import net.fabricmc.api.ClientModInitializer;

public class ModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        EvalTest.hello();
    }
}
