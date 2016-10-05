package io.takari.bpm.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class SubProcess extends ProcessDefinition {

    private static final long serialVersionUID = 1L;

    private final boolean useSeparateContext;
    private final Set<VariableMapping> outVariables;

    public SubProcess(String id, Collection<AbstractElement> children) {
        this(id, false, Collections.emptySet(), children);
    }

    public SubProcess(String id, boolean useSeparateContext, Collection<AbstractElement> children) {
        this(id, useSeparateContext, null, children);
    }
    
    /**
     * @param id
     * @param useSeparateContext if {@code false} use the context of a parent process. All changes to variables will be
     *                           visible to sibling and following processes and tasks. If {@code true} all changes, except 
     *                           those specified in outs, will be discarded.
     * @param outVariables if {@code useSeparateContext} is set to {@code true}, use these variable mappings as out parameters,
     *                     ignored if {@code useSeparateContext} is {@code false}
     * @param children
     */
    public SubProcess(String id, boolean useSeparateContext, Set<VariableMapping> outVariables, Collection<AbstractElement> children) {
        super(id, children);
        this.useSeparateContext = useSeparateContext;
        this.outVariables = outVariables;
    }

    public boolean isUseSeparateContext() {
        return useSeparateContext;
    }
    
    public Set<VariableMapping> getOutVariables() {
        return outVariables;
    }
}
