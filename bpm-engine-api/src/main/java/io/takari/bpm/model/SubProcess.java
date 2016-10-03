package io.takari.bpm.model;

import java.util.Collection;

public class SubProcess extends ProcessDefinition {

    private static final long serialVersionUID = 1L;

    private String name;
    private final boolean useSeparateContext;

    public SubProcess(String id, Collection<AbstractElement> children) {
        this(id, false, children);
    }

    /**
     * @param id
     * @param useSeparateContext if {@code false} use the context of a parent process. All changes to variables will be
     *                           visible to sibling and following processes and tasks. If {@true} all changes will be
     *                           discarded.
     * @param children
     */
    public SubProcess(String id, boolean useSeparateContext, Collection<AbstractElement> children) {
        super(id, children);
        this.useSeparateContext = useSeparateContext;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isUseSeparateContext() {
        return useSeparateContext;
    }
}
