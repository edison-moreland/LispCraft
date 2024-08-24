package net.devdude.lispcraft.mod.common.console;

import com.mojang.serialization.MapCodec;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import net.devdude.lispcraft.mod.Mod;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

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
        @Nullable PtyProcess process;
        boolean done = true;

        public Entity(BlockPos pos, BlockState state) {
            super(Mod.BlockEntities.SHELL_CONSOLE, pos, state);
        }

        @Override
        public void startConsole(InputStream input, OutputStream output) throws IOException {
            String[] cmd = {"/bin/zsh", "-l"};
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("TERM", "xterm");
            var size = this.vt100.getScreen().getSize();
            process = new PtyProcessBuilder()
                    .setInitialColumns(size.width())
                    .setInitialRows(size.height())
                    .setCommand(cmd)
                    .setEnvironment(env)
                    .setRedirectErrorStream(true)
                    .start();

            if (!this.process.isAlive()) {
                throw new IllegalStateException("Shell console process not running");
            }
            Mod.LOGGER.info("Shell console process started {}", this.getPos());
            Thread.startVirtualThread(this.process.onExit().whenComplete((process, ex) -> {
                Mod.LOGGER.info("Shell console process exiting {}", this.getPos());
                if (!this.done) {
                    this.done = true;
                }
            })::join);
            this.done = false;

            copyStream(this.process.getInputStream(), output);
            copyStream(input, this.process.getOutputStream());
        }

        @Override
        public void endConsole() {
            this.done = true;
            if (this.process != null && this.process.isAlive()) {
                Mod.LOGGER.info("Shell console process ending");
                this.process.destroy();
                this.process.destroyForcibly();
                this.process = null;
            }
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
                    // Process probably ended
                    this.endConsole();
                }
            });
        }
    }
}
