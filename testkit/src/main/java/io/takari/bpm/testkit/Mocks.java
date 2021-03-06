package io.takari.bpm.testkit;

import io.takari.bpm.task.ServiceTaskRegistry;
import java.util.HashMap;
import java.util.Map;

public final class Mocks {

    private static final Map<String, Object> items = new HashMap<>();
    
    public static void register(String k, Object v) {
        items.put(k, v);
    }
    
    public static Object get(String k) {
        return items.get(k);
    }
    
    private Mocks() {
    }
    
    public static class Registry implements ServiceTaskRegistry {

        public void register(String key, Object instance) {
            Mocks.register(key, instance);
        }

        @Override
        public Object getByKey(String key) {
            return Mocks.get(key);
        }
    }
}
