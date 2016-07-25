package io.takari.bpm.state;

import com.github.andrewoma.dexx.collection.HashMap;
import com.github.andrewoma.dexx.collection.Map;
import io.takari.bpm.IndexedProcessDefinition;

import java.io.Serializable;

public class Definitions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, IndexedProcessDefinition> defs;

    public Definitions(IndexedProcessDefinition initial) {
        Map<String, IndexedProcessDefinition> m = HashMap.empty();
        this.defs = m.put(initial.getId(), initial);
    }

    private Definitions(Map<String, IndexedProcessDefinition> defs) {
        this.defs = defs;
    }

    public Definitions put(IndexedProcessDefinition pd) {
        return new Definitions(defs.put(pd.getId(), pd));
    }

    public IndexedProcessDefinition get(String key) {
        return defs.get(key);
    }
}
