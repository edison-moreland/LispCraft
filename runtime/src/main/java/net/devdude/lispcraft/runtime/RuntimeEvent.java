package net.devdude.lispcraft.runtime;

// Runtime events need to be easily serializable, which is why they're in java

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.util.Map;

public interface RuntimeEvent {
    //    TODO: Auto most of this
    Map<String, Endec<? extends RuntimeEvent>> REGISTRY = Map.of(
            "Print", Print.ENDEC
    );
    Endec<RuntimeEvent> ENDEC = Endec.dispatched(REGISTRY::get, RuntimeEvent::id, Endec.STRING);

    String id();

    @FunctionalInterface
    interface RuntimeEventHandler {
        void handle(RuntimeEvent event);
    }

    record Print(char[] text) implements RuntimeEvent {
        public static final Endec<Print> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(Print.class);

        @Override
        public String id() {
            return "Print";
        }
    }
}
