package io.takari.bpm.state;

import io.takari.bpm.IndexedProcessDefinition;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.io.Serializable;

public class Definitions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PMap<String, IndexedProcessDefinition> defs;

    public Definitions(IndexedProcessDefinition initial) {
        PMap<String, IndexedProcessDefinition> m = HashTreePMap.empty();
        this.defs = m.plus(initial.getId(), initial);
    }

    private Definitions(PMap<String, IndexedProcessDefinition> defs) {
        this.defs = defs;
    }

    public Definitions put(IndexedProcessDefinition pd) {
        return new Definitions(defs.plus(pd.getId(), pd));
    }

    public IndexedProcessDefinition get(String key) {
        return defs.get(key);
    }
}
