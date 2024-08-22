package net.devdude.lispcraft.mod.common.console;

import io.wispforest.owo.client.screens.SyncedProperty;
import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.mod.common.vt100.Size;
import net.devdude.lispcraft.mod.common.vt100.VT100;
import net.devdude.lispcraft.mod.common.vt100.VT100ScreenBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ConsoleScreenHandler extends ScreenHandler {
    public SyncedProperty<VT100ScreenBuffer> screen;

    // Called by the client
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    // Called by the server
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory, @Nullable VT100 vt100) {
        super(Mod.ScreenHandlers.CONSOLE, syncId);

        this.addServerboundMessage(KeyboardInputPacket.class, packet -> {
            assert vt100 != null;
            try {
                vt100.writeKeyboardInput(packet.input);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        this.screen = this.createProperty(VT100ScreenBuffer.class, VT100ScreenBuffer.ENDEC, new VT100ScreenBuffer(new Size(40, 20)));

        if (vt100 != null) {
            screen.set(vt100.getScreen());

            // TODO: We have no way to remove an observer from here.
            //       Will the block entity just accumulate dead observers? Are they cleaned up somehow?
            vt100.onChange(((screen) -> {
                this.screen.set(screen);
            }));
        }
    }

    @Environment(EnvType.CLIENT)
    public void writeInput(char character) {
        writeInput(new byte[]{(byte) character});
    }

    @Environment(EnvType.CLIENT)
    public void writeInput(byte[] bytes) {
        this.sendMessage(new KeyboardInputPacket(bytes));
    }

    @Environment(EnvType.CLIENT)
    public void writeInput(int character) {
        writeInput(new byte[]{(byte) character});
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public record KeyboardInputPacket(byte[] input) {
    }
}
