package io.takari.bpm.state;

import java.io.Serializable;

import org.organicdesign.fp.collections.ImMap;
import org.organicdesign.fp.collections.PersistentHashMap;

import io.takari.bpm.IndexedProcessDefinition;

public class Definitions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ImMap<String, IndexedProcessDefinition> defs;

    public Definitions(IndexedProcessDefinition initial) {
        ImMap<String, IndexedProcessDefinition> m = PersistentHashMap.empty();
        this.defs = m.assoc(initial.getId(), initial);
    }

    private Definitions(ImMap<String, IndexedProcessDefinition> defs) {
        this.defs = defs;
    }

    public Definitions put(IndexedProcessDefinition pd) {
        return new Definitions(defs.assoc(pd.getId(), pd));
    }

    public IndexedProcessDefinition get(String key) {
        return defs.get(key);
    }
}
