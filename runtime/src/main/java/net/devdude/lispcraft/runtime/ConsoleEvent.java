package net.devdude.lispcraft.runtime;

// Runtime events need to be easily serializable, which is why they're in java

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.util.Map;

public interface ConsoleEvent {
    //    TODO: Auto most of this
    Map<String, Endec<? extends ConsoleEvent>> REGISTRY = Map.of(
            "Write", Write.ENDEC
    );
    Endec<ConsoleEvent> ENDEC = Endec.dispatched(REGISTRY::get, ConsoleEvent::id, Endec.STRING);

    String id();

    @FunctionalInterface
    interface RuntimeEventHandler {
        void handle(ConsoleEvent event);
    }

    record Write(byte[] bytes) implements ConsoleEvent {
        public static final Endec<Write> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(Write.class);

        @Override
        public String id() {
            return "Write";
        }
    }
}
