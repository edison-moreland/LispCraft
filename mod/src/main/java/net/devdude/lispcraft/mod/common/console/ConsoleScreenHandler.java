package net.devdude.lispcraft.mod.common.console;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.RecordEndec;
import io.wispforest.owo.client.screens.SyncedProperty;
import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.runtime.Console;
import net.devdude.lispcraft.runtime.ConsoleEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

public class ConsoleScreenHandler extends ScreenHandler {
    public SyncedProperty<char[][]> buffer;
    public SyncedProperty<Console.Location> cursor;

    // Called by the client
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null, null);
    }

    // Called by the server
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory, @Nullable Console console, @Nullable ConsoleEvent.RuntimeEventHandler eventHandler) {
        super(Mod.ScreenHandlers.CONSOLE, syncId);

        this.addServerboundMessage(RuntimeEventPacket.class, RuntimeEventPacket.ENDEC, event -> {
            assert eventHandler != null;
            eventHandler.handle(event.event);
        });

        buffer = this.createProperty(char[][].class, new char[20][40]);
        cursor = this.createProperty(Console.Location.class, new Console.Location(0, 0));

        if (console != null) {
//            Only the server passes in the screen
            this.buffer.set(console.getScreen());

//            TODO: We have no way to remove an observer from here.
//                  Will the block entity just accumulate dead observers? Are they cleaned up somehow?
            console.onChange(((buffer, cursor) -> {
                this.buffer.set(buffer);
                this.cursor.set(cursor);
            }));
        } else {
        }
    }

    @Environment(EnvType.CLIENT)
    public void writeBytes(byte[] bytes) {
        this.sendMessage(new RuntimeEventPacket(new ConsoleEvent.Write(bytes)));
    }

    @Environment(EnvType.CLIENT)
    public void writeCharacter(char character) {
        this.sendMessage(new RuntimeEventPacket(new ConsoleEvent.Write(new byte[]{(byte) character})));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public record RuntimeEventPacket(ConsoleEvent event) {
        public static final Endec<RuntimeEventPacket> ENDEC = RecordEndec.createShared(RuntimeEventPacket.class);
    }
}
