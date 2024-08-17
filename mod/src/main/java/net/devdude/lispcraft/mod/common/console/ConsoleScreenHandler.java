package net.devdude.lispcraft.mod.common.console;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.RecordEndec;
import io.wispforest.owo.client.screens.SyncedProperty;
import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.runtime.Console;
import net.devdude.lispcraft.runtime.RuntimeEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

public class ConsoleScreenHandler extends ScreenHandler {
    public SyncedProperty<char[][]> characters;


    // Called by the client
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null, null);
    }

    // Called by the server
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory, @Nullable Console console, @Nullable RuntimeEvent.RuntimeEventHandler eventHandler) {
        super(Mod.ScreenHandlers.CONSOLE, syncId);

        this.addServerboundMessage(RuntimeEventPacket.class, RuntimeEventPacket.ENDEC, event -> {
            assert eventHandler != null;
            eventHandler.handle(event.event);
        });

        if (console != null) {
//            Only the server passes in the screen
            characters = this.createProperty(char[][].class, console.getScreen());
            characters.markDirty();

//            TODO: We have no way to remove an observer from here. Will the block entity just accumulate dead observers? Are they cleaned up somehow?
            console.observe(this.characters::set);
        } else {
            characters = this.createProperty(char[][].class, new char[20][40]);
        }
    }

    @Environment(EnvType.CLIENT)
    public void sendRuntimeEvent(RuntimeEvent event) {
        this.sendMessage(new RuntimeEventPacket(event));
    }

    @Environment(EnvType.CLIENT)
    public void sendKeyPressedEvent(int keyCode, int modifiers) {
        this.sendRuntimeEvent(new RuntimeEvent.KeyPressed(keyCode, modifiers));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public record RuntimeEventPacket(RuntimeEvent event) {
        public static final Endec<RuntimeEventPacket> ENDEC = RecordEndec.createShared(RuntimeEventPacket.class);
    }
}
