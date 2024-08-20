package net.devdude.lispcraft.mod.common.console;

import com.mojang.serialization.MapCodec;
import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.runtime.ConsoleRuntime;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

public class ClojureConsoleBlock extends ConsoleBlock {
    public static final MapCodec<ConsoleBlock> CODEC = createCodec(ShellConsoleBlock::new);

    public ClojureConsoleBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    public static class Entity extends ConsoleBlockEntity {
        @Environment(EnvType.SERVER)
        @Nullable ConsoleRuntime runtime;

        public Entity(BlockPos pos, BlockState state) {
            super(Mod.BlockEntities.CLOJURE_CONSOLE, pos, state);
        }

        @Override
        public void startConsole(InputStream input, OutputStream output) {
            assert this.runtime == null;
            this.runtime = new ConsoleRuntime();
            this.runtime.start(input, output);
        }

        @Override
        public void endConsole() {
            assert this.runtime != null;
            this.runtime.stop();
        }
    }
}