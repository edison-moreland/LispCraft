package net.devdude.lispcraft.mod.common.vt100;

import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.mod.common.vt100.ansi.ANSI;
import net.devdude.lispcraft.mod.common.vt100.ansi.C1;
import net.devdude.lispcraft.mod.common.vt100.ansi.CSI;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

public class EscapeParser {
    private final HashMap<ControlFunction, ControlFunctionHandler> controlFunctions = new HashMap<>();
    private final ByteArrayOutputStream sequence = new ByteArrayOutputStream();
    private final Deque<byte[]> stack = new ArrayDeque<>();

    public boolean isStartOfEscape(int c) {
        return ANSI.isControlCharacter(c);
    }

    public void register(ControlFunction controlFunction, ControlFunctionHandler handler) {
        this.controlFunctions.put(controlFunction, handler);
    }

    public void parse(int first, Reader reader) {
        this.sequence.write(first);
        switch (first) {
            case ANSI.LF -> finishSequence(ControlFunction.LF);
            case ANSI.BS -> finishSequence(ControlFunction.BS);
            case ANSI.TAB -> finishSequence(ControlFunction.TAB);
            case ANSI.CR -> finishSequence(ControlFunction.CR);
            case ANSI.ESC -> parseC1(reader);
            default -> finishSequence(null);
        }
    }

    private void finishSequence(@Nullable ControlFunction controlFunction) {
        if (controlFunction != null) {
            Mod.LOGGER.info("Control function {} {}", controlFunction.name, sequence.toByteArray());

            var handler = this.controlFunctions.get(controlFunction);
            if (handler != null) {
                handler.handle(stack.toArray(new byte[][]{}));
            }
        }

        sequence.reset();
        stack.clear();
    }

    private void parseC1(Reader reader) {
        var next = reader.read();
        this.sequence.write(next);
        switch (next) {
            case C1.CSI -> parseCSI(reader);
            // case ANSI.ESC -> finishSequence(null);
            default -> finishSequence(null);
        }
    }

    private void parseCSI(Reader reader) {
        // Read control sequence
        var finalByte = this.parseCSIArgs(reader);
        var decPrivate = (this.stack.peekLast()[0] == '?');

        switch (finalByte) {
            case CSI.SGR -> this.finishSequence(ControlFunction.SGR);
            case CSI.ED -> {
                if (decPrivate) {
                    this.finishSequence(ControlFunction.DECSED);
                } else {
                    this.finishSequence(ControlFunction.ED);
                }
            }
            case CSI.EL -> {
                if (decPrivate) {
                    this.finishSequence(ControlFunction.DECSEL);
                } else {
                    this.finishSequence(ControlFunction.EL);
                }
            }
            case CSI.CUP -> this.finishSequence(ControlFunction.CUP);
            case CSI.CUB -> this.finishSequence(ControlFunction.CUB);
            case CSI.CUF -> this.finishSequence(ControlFunction.CUF);
            case CSI.CUD -> this.finishSequence(ControlFunction.CUD);
            case CSI.CUU -> this.finishSequence(ControlFunction.CUU);
            case CSI.DSR -> {
                if (decPrivate) {
                    this.finishSequence(ControlFunction.DECDSR);
                } else {
                    this.finishSequence(ControlFunction.DSR);
                }
            }
            case CSI.SM -> {
                if (decPrivate) {
                    this.finishSequence(ControlFunction.DECSET);
                } else {
                    this.finishSequence(ControlFunction.SM);
                }
            }
            case CSI.RM -> {
                if (decPrivate) {
                    this.finishSequence(ControlFunction.DECRST);
                } else {
                    this.finishSequence(ControlFunction.RM);
                }
            }
            default -> {

            }
        }
    }

    private byte parseCSIArgs(Reader reader) {
        // parameter byte range = 0x30–0x3F
        // intermediate byte range = 0x20–0x2F
        // final byte range = 0x40–0x7E

        var phase = 0; // 0 = looking for parameter, 1 = intermediate, 2 = final
        var parameterBuffer = new ByteArrayOutputStream();
        while (true) {
            var next = reader.read();
            this.sequence.write(next);
            parameterBuffer.write(next);

            if (phase == 0 && !isInRange(next, 0x30, 0x3F)) {
                this.stack.push(parameterBuffer.toByteArray());
                parameterBuffer.reset();
                phase = 1;
            }

            if (phase == 1 && !isInRange(next, 0x20, 0x2F)) {
                this.stack.push(parameterBuffer.toByteArray());
                parameterBuffer.reset();
                phase = 2;
            }

            if (next >= 0x40 && next <= 0x7E) {
                if (phase != 2) {
                    throw new RuntimeException("This shouldn't happen, you messed up");
                }

                return (byte) next;
            }
        }
    }

    private static boolean isInRange(int v, int low, int high) {
        return low <= v && v <= high;
    }

    public enum ControlFunction {
        ACL("Acknowledge"),
        APC("Application Program Command"),
        BEL("Bell"),
        BPH("Break Permitted Here"),
        BS("Backspace"),
        CAN("Cancel"),
        CBT("Cursor Backward Tabulation"),
        CCH("Cancel Character"),
        CHA("Cursor Character Absolute"),
        CHT("Cursor Forward Tabulation"),
        CMD("Coding Method Delimiter"),
        CNL("Cursor Next Line"),
        CPL("Cursor Preceding Line"),
        CPR("Active Cursor Position Report"),
        CR("Carriage Return"),
        TAB("Tab"),
        LF("Line Feed"),
        SGR("Select Graphics Rendition"),
        SM("Set Mode"),
        DECSET("DEC Private Set Mode"),
        RM("Reset Mode"),
        DECRST("DEC Private Reset Mode"),
        DSR("Device Status Report"),
        DECDSR("DEC Private Device Status Report"),
        CUU("Cursor Up"),
        CUD("Cursor Down"),
        CUF("Cursor Forward"),
        CUB("Cursor Backward"),
        CUP("Cursor Position"),
        EL("Erase in Line"),
        DECSEL("DEC Private Erase in Line"),
        ED("Erase in Display"),
        DECSED("DEC Private Erase in Display"),
        ;

        public final String name;

        ControlFunction(String name) {
            this.name = name;
        }
    }

    @FunctionalInterface
    public interface Reader {
        int read();
    }

    @FunctionalInterface
    public interface ControlFunctionHandler {
        void handle(byte[][] args);
    }
}
