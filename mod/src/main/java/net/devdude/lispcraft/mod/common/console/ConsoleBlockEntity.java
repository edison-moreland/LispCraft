package net.devdude.lispcraft.mod.common.console;

import net.devdude.lispcraft.mod.common.vt100.Size;
import net.devdude.lispcraft.mod.common.vt100.VT100;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ConsoleBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    @Environment(EnvType.SERVER)
    @Nullable
    VT100 vt100;

    public ConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (!world.isClient()) {
            this.vt100 = new VT100(new Size(40, 20));
            try {
                this.vt100.start();
                this.startConsole(vt100.getInputStream(), vt100.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, serverWorld) -> {
                if (blockEntity == this) {
                    this.endConsole();
                    this.vt100.stop();
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
        return new ConsoleScreenHandler(syncId, playerInventory, this.vt100);
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Console");
    }
}
