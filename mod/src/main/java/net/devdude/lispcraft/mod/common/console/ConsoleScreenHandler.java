package net.devdude.lispcraft.mod.common.console;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.RecordEndec;
import io.wispforest.owo.client.screens.SyncedProperty;
import io.wispforest.owo.util.Observable;
import net.devdude.lispcraft.mod.Mod;
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

    public record RuntimeEventPacket(RuntimeEvent event) {
        public static final Endec<RuntimeEventPacket> ENDEC = RecordEndec.createShared(RuntimeEventPacket.class);
    }

    // Called by the server
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory, @Nullable Observable<char[][]> screen, @Nullable RuntimeEvent.RuntimeEventHandler eventHandler) {
        super(Mod.ScreenHandlers.CONSOLE, syncId);

        this.addServerboundMessage(RuntimeEventPacket.class, RuntimeEventPacket.ENDEC, event -> {
            assert eventHandler != null;
            eventHandler.handle(event.event);
        });

        if (screen != null) {
//            Only the server passes in the screen
            characters = this.createProperty(char[][].class, screen.get());
            characters.markDirty();
            screen.observe(this.characters::set);
        } else {
            characters = this.createProperty(char[][].class, new char[ConsoleBlockEntity.charsY][ConsoleBlockEntity.charsX]);
        }
    }

    @Environment(EnvType.CLIENT)
    public void sendRuntimeEvent(RuntimeEvent event) {
        this.sendMessage(new RuntimeEventPacket(event));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
