package com.wiredid.skytree.minion;

import com.wiredid.skytree.model.MinionType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import com.wiredid.skytree.model.MinionData;

public class MinionRegistry {
    private static final Map<MinionType, Consumer<MinionData>> handlers = new HashMap<>();

    public static void register(MinionType type, Consumer<MinionData> handler) {
        handlers.put(type, handler);
    }

    public static void execute(MinionData data) {
        Consumer<MinionData> handler = handlers.get(data.getType());
        if (handler != null) {
            handler.accept(data);
        }
    }
}
