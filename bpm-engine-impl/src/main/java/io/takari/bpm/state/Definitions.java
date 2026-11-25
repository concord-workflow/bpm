package io.takari.bpm.state;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.api.ExecutionException;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.io.Serializable;
import java.util.Map;

/**
 * Process definitions used by a process instance (the main process and the called processes).
 * <p>
 * The definitions are either embedded into the state or stored as references (definition ID and
 * fingerprint, see {@link #toReferences()}) and resolved when the state is loaded.
 */
public class Definitions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PMap<String, IndexedProcessDefinition> defs;

    /**
     * Definition ID to fingerprint. {@code null} unless the definitions are stored as references.
     */
    private final PMap<String, String> refs;

    public Definitions(IndexedProcessDefinition initial) {
        PMap<String, IndexedProcessDefinition> m = HashTreePMap.empty();
        this.defs = m.plus(initial.getId(), initial);
        this.refs = null;
    }

    private Definitions(PMap<String, IndexedProcessDefinition> defs, PMap<String, String> refs) {
        this.defs = defs;
        this.refs = refs;
    }

    public Definitions put(IndexedProcessDefinition pd) {
        return new Definitions(defs.plus(pd.getId(), pd), refs != null ? refs.minus(pd.getId()) : null);
    }

    public IndexedProcessDefinition get(String key) {
        return defs.get(key);
    }

    /**
     * @return {@code true} if some definitions are stored as references and must be resolved.
     */
    public boolean hasReferences() {
        return refs != null && !refs.isEmpty();
    }

    /**
     * @return a copy of the definitions without the definitions themselves, only their IDs and fingerprints.
     */
    public Definitions toReferences() {
        PMap<String, String> m = refs != null ? refs : HashTreePMap.empty();
        for (Map.Entry<String, IndexedProcessDefinition> e : defs.entrySet()) {
            m = m.plus(e.getKey(), e.getValue().getFingerprint());
        }
        return new Definitions(HashTreePMap.empty(), m);
    }

    /**
     * Resolves the references using the specified provider.
     *
     * @throws ExecutionException if a definition can't be found or its content was changed.
     */
    public Definitions resolve(Resolver resolver) throws ExecutionException {
        if (!hasReferences()) {
            return this;
        }

        PMap<String, IndexedProcessDefinition> m = defs;
        for (Map.Entry<String, String> e : refs.entrySet()) {
            String id = e.getKey();
            IndexedProcessDefinition pd = resolver.resolve(id);
            if (pd == null) {
                throw new ExecutionException("Process definition not found: %s", id);
            }

            if (!e.getValue().equals(pd.getFingerprint())) {
                throw new ExecutionException("Process definition '%s' was changed after the process was suspended. " +
                        "Use a new definition ID for each version of the definition", id);
            }

            m = m.plus(id, pd);
        }

        return new Definitions(m, null);
    }

    public interface Resolver {

        IndexedProcessDefinition resolve(String id) throws ExecutionException;
    }
}
