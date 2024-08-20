package net.devdude.lispcraft.mod.common.console;

import io.wispforest.owo.client.screens.SyncedProperty;
import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.mod.common.vt100.VT100Emulator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ConsoleScreenHandler extends ScreenHandler {
    public SyncedProperty<char[][]> buffer;
    public SyncedProperty<VT100Emulator.Location> cursor;

    // Called by the client
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    // Called by the server
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory, @Nullable VT100Emulator vt100) {
        super(Mod.ScreenHandlers.CONSOLE, syncId);

        this.addServerboundMessage(KeyboardInputPacket.class, packet -> {
            assert vt100 != null;
            try {
                vt100.writeKeyboardInput(packet.input);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        buffer = this.createProperty(char[][].class, new char[20][40]);
        cursor = this.createProperty(VT100Emulator.Location.class, new VT100Emulator.Location(0, 0));

        if (vt100 != null) {
            // Only the server passes in the screen
            this.buffer.set(vt100.getScreen());
            this.cursor.set(vt100.getCursor());

            // TODO: We have no way to remove an observer from here.
            //       Will the block entity just accumulate dead observers? Are they cleaned up somehow?
            vt100.onChange(((buffer, cursor) -> {
                this.buffer.set(buffer);
                this.cursor.set(cursor);
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
