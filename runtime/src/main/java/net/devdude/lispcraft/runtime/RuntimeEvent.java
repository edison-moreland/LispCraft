package net.devdude.lispcraft.runtime;

// Runtime events need to be easily serializable, which is why they're in java

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.util.Map;

public interface RuntimeEvent {
    //    TODO: Auto most of this
    Map<String, Endec<? extends RuntimeEvent>> REGISTRY = Map.of(
//            "ButtonPush", ButtonPush.ENDEC,
            "PrintLine", PrintLine.ENDEC,
            "KeyPressed", KeyPressed.ENDEC
    );
    Endec<RuntimeEvent> ENDEC = Endec.dispatched(REGISTRY::get, RuntimeEvent::id, Endec.STRING);

    String id();

    @FunctionalInterface
    interface RuntimeEventHandler {
        void handle(RuntimeEvent event);
    }

//    record ButtonPush() implements RuntimeEvent {
//        public static final Endec<ButtonPush> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(ButtonPush.class);
//
//        @Override
//        public String id() {
//            return "ButtonPush";
//        }
//    }

    record PrintLine(String text) implements RuntimeEvent {
        public static final Endec<PrintLine> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(PrintLine.class);

        @Override
        public String id() {
            return "PrintLine";
        }
    }

    record KeyPressed(int keyCode, int modifiers) implements RuntimeEvent {
        public static final Endec<KeyPressed> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(KeyPressed.class);

        @Override
        public String id() {
            return "KeyPressed";
        }
    }
}
