package net.devdude.lispcraft.mod.common.console;

import io.wispforest.owo.util.Observable;
import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.mod.Network;
import net.devdude.lispcraft.runtime.CharacterScreen;
import net.devdude.lispcraft.runtime.ConsoleRuntime;
import net.devdude.lispcraft.runtime.RuntimeEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ConsoleBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, CharacterScreen {
    public static int charsX = 40;
    public static int charsY = 20;
    @Environment(EnvType.SERVER)
    @Nullable ConsoleRuntime runtime;
    Observable<char[][]> screen = Observable.of(blankScreen());

    public ConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(Mod.BlockEntities.CONSOLE, pos, state);
    }

    public static char[][] blankScreen() {
        var screen = new char[charsY][charsX];
        for (int i = 0; i < charsY; i++) {
            for (int j = 0; j < charsX; j++) {
                screen[i][j] = ' ';
            }
        }
        return screen;
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);

        if (!world.isClient()) {
            this.runtime = new ConsoleRuntime();
            this.runtime.start(this);
        } else {
            Network.sendPing("Pong");
        }
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ConsoleScreenHandler(syncId, playerInventory, this.screen, this::handle);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }


    @Environment(EnvType.SERVER)
    public void print(int atX, int atY, String text) {
        System.out.println(atX + " " + atY + " " + text);
        if (atY >= charsY) {
            return;
        }

        var screen = this.screen.get().clone();
        var maxX = Integer.min(charsX, atX + text.length());
        for (int x = atX; x < maxX; x++) {
            var i = x - atX;

            screen[atY][x] = text.charAt(i);
        }
        this.screen.set(screen);
    }

    @Environment(EnvType.SERVER)
    public void handle(RuntimeEvent event) {
        assert this.runtime != null;
        this.runtime.sendRuntimeEvent(event);
    }
}
