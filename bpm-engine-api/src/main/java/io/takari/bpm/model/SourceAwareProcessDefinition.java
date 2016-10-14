package io.takari.bpm.model;

import java.util.Collection;
import java.util.Map;

public class SourceAwareProcessDefinition extends ProcessDefinition {

    private final Map<String, SourceMap> sourceMaps;

    public SourceAwareProcessDefinition(ProcessDefinition source, Map<String, SourceMap> sourceMaps) {
        super(source, source.getAttributes());
        this.sourceMaps = sourceMaps;
    }

    public SourceAwareProcessDefinition(String id, Collection<AbstractElement> children, Map<String, String> attributes, Map<String, SourceMap> sourceMaps) {
        super(id, children, attributes);
        this.sourceMaps = sourceMaps;
    }

    public Map<String, SourceMap> getSourceMaps() {
        return sourceMaps;
    }
}
