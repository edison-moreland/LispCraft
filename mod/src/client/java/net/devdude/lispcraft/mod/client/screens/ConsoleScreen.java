package net.devdude.lispcraft.mod.client.screens;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.devdude.lispcraft.mod.client.components.CharGridComponent;
import net.devdude.lispcraft.mod.common.console.ConsoleBlockEntity;
import net.devdude.lispcraft.mod.common.console.ConsoleScreenHandler;
import net.devdude.lispcraft.runtime.RuntimeEvent;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class ConsoleScreen extends BaseOwoScreen<FlowLayout> implements ScreenHandlerProvider<ConsoleScreenHandler> {
    private final ConsoleScreenHandler handler;

    public ConsoleScreen(ConsoleScreenHandler handler, PlayerInventory inventory, Text title) {
        super(title);
        this.handler = handler;
    }

    @Override
    public ConsoleScreenHandler getScreenHandler() {
        return handler;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        rootComponent.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(new CharGridComponent(ConsoleBlockEntity.charsX, ConsoleBlockEntity.charsY, this.handler.characters::get))
                        .child(Components.button(Text.literal("Click me!"), button -> getScreenHandler().sendRuntimeEvent(new RuntimeEvent.PrintLine("hello"))))
                        .padding(Insets.of(10))
                        .surface(Surface.DARK_PANEL)
                        .verticalAlignment(VerticalAlignment.CENTER)
                        .horizontalAlignment(HorizontalAlignment.CENTER)
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!super.keyPressed(keyCode, scanCode, modifiers)) {
            getScreenHandler().sendKeyPressedEvent(keyCode, scanCode);
        }
        ;

        return true;
    }

}
