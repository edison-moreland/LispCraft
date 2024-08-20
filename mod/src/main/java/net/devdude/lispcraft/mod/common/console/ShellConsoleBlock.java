package net.devdude.lispcraft.mod.common.console;

import com.mojang.serialization.MapCodec;
import net.devdude.lispcraft.mod.Mod;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ShellConsoleBlock extends ConsoleBlock {
    public static final MapCodec<ConsoleBlock> CODEC = createCodec(ShellConsoleBlock::new);

    public ShellConsoleBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    public static class Entity extends ConsoleBlockEntity {
        @Nullable Process process;
        boolean done = true;

        public Entity(BlockPos pos, BlockState state) {
            super(Mod.BlockEntities.SHELL_CONSOLE, pos, state);
        }

        @Override
        public void startConsole(InputStream input, OutputStream output) throws IOException {
            this.process = new ProcessBuilder("/bin/sh").start();
            if (!this.process.isAlive()) {
                throw new IllegalStateException("Shell console process not running");
            }
            this.done = false;
            this.process.onExit().whenComplete((process, ex) -> this.done = true);

            copyStream(this.process.getInputStream(), output);
            copyStream(this.process.getErrorStream(), output);
            copyStream(input, this.process.getOutputStream());
        }

        private void copyStream(InputStream in, OutputStream out) {
            Thread.startVirtualThread(() -> {
                try {
                    var buffer = new byte[255];
                    while (!this.done) {
                        var read = in.read(buffer);
                        if (read != -1) {
                            out.write(buffer, 0, read);
                            out.flush();
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public void endConsole() {
            this.done = true;
            if (this.process != null && this.process.isAlive()) {
                this.process.destroy();
            }
        }
    }
}
