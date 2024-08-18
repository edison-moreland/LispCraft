package net.devdude.lispcraft.mod.common.console;

import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.mod.Network;
import net.devdude.lispcraft.runtime.Console;
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

public class ConsoleBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    @Environment(EnvType.SERVER)
    @Nullable ConsoleRuntime runtime;

    Console console = new Console(new Console.Size(40, 20));

    public ConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(Mod.BlockEntities.CONSOLE, pos, state);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);

        if (!world.isClient()) {
            assert this.console != null;
            this.runtime = new ConsoleRuntime();
            this.runtime.start(this.console);
        } else {
            Network.sendPing("Pong");
        }
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ConsoleScreenHandler(syncId, playerInventory, this.console, this::handle);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Environment(EnvType.SERVER)
    public void handle(RuntimeEvent event) {
        assert this.runtime != null;
        this.runtime.sendRuntimeEvent(event);
    }
}
