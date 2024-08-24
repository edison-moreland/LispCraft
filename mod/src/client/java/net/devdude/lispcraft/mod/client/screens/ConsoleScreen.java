package net.devdude.lispcraft.mod.client.screens;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.devdude.lispcraft.mod.client.components.VT100ScreenComponent;
import net.devdude.lispcraft.mod.common.console.ConsoleScreenHandler;
import net.devdude.lispcraft.mod.common.vt100.ansi.ANSI;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.io.ByteArrayOutputStream;

public class ConsoleScreen extends BaseOwoScreen<FlowLayout> implements ScreenHandlerProvider<ConsoleScreenHandler> {
    private final ConsoleScreenHandler handler;

    public ConsoleScreen(ConsoleScreenHandler handler, PlayerInventory inventory, Text title) {
        super(title);
        this.handler = handler;
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
                        .child(new VT100ScreenComponent(this.handler.screen::get))
                        .padding(Insets.of(10))
                        .surface(Surface.DARK_PANEL)
                        .verticalAlignment(VerticalAlignment.CENTER)
                        .horizontalAlignment(HorizontalAlignment.CENTER)
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        var sequence = new ByteArrayOutputStream();
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> sequence.write(ANSI.BS);
            case GLFW.GLFW_KEY_TAB -> sequence.write(ANSI.TAB);
            case GLFW.GLFW_KEY_ENTER -> sequence.write(ANSI.CR);
            case GLFW.GLFW_KEY_UP -> {
                sequence.write(ANSI.ESC);
                sequence.write('A');
            }
            case GLFW.GLFW_KEY_DOWN -> {
                sequence.write(ANSI.ESC);
                sequence.write('B');
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                sequence.write(ANSI.ESC);
                sequence.write('C');
            }
            case GLFW.GLFW_KEY_LEFT -> {
                sequence.write(ANSI.ESC);
                sequence.write('D');
            }
            default -> {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE && (((modifiers & GLFW.GLFW_MOD_ALT) == GLFW.GLFW_MOD_ALT))) {
                    sequence.write(ANSI.ESC);
                    sequence.write(ANSI.ESC);
                } else {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }

        getScreenHandler().writeInput(sequence.toByteArray());
        return true;
    }

    @Override
    public ConsoleScreenHandler getScreenHandler() {
        return handler;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!super.charTyped(chr, modifiers)) {
            getScreenHandler().writeInput(new byte[]{(byte) chr});
        }

        return true;
    }
}
