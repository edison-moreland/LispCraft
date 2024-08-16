package net.devdude.lispcraft.mod.common.console;

import io.wispforest.owo.client.screens.SyncedProperty;
import io.wispforest.owo.util.Observable;
import net.devdude.lispcraft.mod.Mod;
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
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory, @Nullable Observable<char[][]> screen, @Nullable ConsoleBlockEntity.ClientEventHandler eventHandler) {
        super(Mod.ScreenHandlers.CONSOLE, syncId);

        this.addServerboundMessage(ConsoleBlockEntity.ClientEvent.class, event -> {
            assert eventHandler != null;
            eventHandler.handleEvent(event);
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
    public void sendClientEvent(ConsoleBlockEntity.ClientEvent event) {
        this.sendMessage(event);
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
