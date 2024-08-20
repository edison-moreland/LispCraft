package net.devdude.lispcraft.mod.common.console;

import net.devdude.lispcraft.mod.common.vt100.VT100Emulator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.*;

public abstract class ConsoleBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    @Environment(EnvType.SERVER)
    VT100Emulator vt100 = new VT100Emulator(new VT100Emulator.Size(40, 20));
    // Used to write to the stdin of the process attached to this console
    @Environment(EnvType.SERVER)
    PipedOutputStream processStdin = new PipedOutputStream();

    public ConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setWorld(World world) {
        if (!world.isClient()) {
            try {
                this.startConsole(new PipedInputStream(processStdin), vt100);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, serverWorld) -> {
                if (blockEntity == this) {
                    this.endConsole();
                }
            });
        }
    }

    // Called by the block entity is loaded
    public abstract void startConsole(InputStream input, OutputStream output) throws IOException;

    // Called by the block entity is unloaded
    public abstract void endConsole();

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ConsoleScreenHandler(syncId, playerInventory, this.vt100, this::handle);
    }

    @Environment(EnvType.SERVER)
    public void handle(ConsoleEvent event) {
        switch (event) {
            case ConsoleEvent.Write w:
                try {
                    this.processStdin.write(w.bytes());
                    this.processStdin.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected event: " + event);
        }
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Console");
    }
}
