package io.takari.bpm.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapUtils {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepMerge(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> result = new LinkedHashMap<>(a != null ? a : Collections.emptyMap());

        for (String k : b.keySet()) {
            Object av = result.get(k);
            Object bv = b.get(k);

            // this is necessary to preserve the order of the keys
            result.remove(k);

            if (av instanceof Map && bv instanceof Map) {
                result.put(k, deepMerge((Map<String, Object>) av, (Map<String, Object>) bv));
            } else {
                result.put(k, bv);
            }
        }
        return result;
    }
}
