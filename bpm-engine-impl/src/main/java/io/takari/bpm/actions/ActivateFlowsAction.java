package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.pcollections.PSequence;
import org.pcollections.TreePVector;

public class ActivateFlowsAction implements Action {

    private static final long serialVersionUID = 1L;

    public static ActivateFlowsAction empty(String definitionId) {
        return new ActivateFlowsAction(definitionId, TreePVector.empty());
    }

    private final String definitionId;
    private final PSequence<Flow> flows;

    public ActivateFlowsAction(String definitionId, String elementId, int count) {
        this(definitionId, Collections.singleton(new Flow(elementId, count)));
    }

    public ActivateFlowsAction(String definitionId, Collection<String> elementIds, int count) {
        this(definitionId, elementIds.stream().map(id -> new Flow(id, count)).collect(Collectors.toList()));
    }

    public ActivateFlowsAction(String definitionId, Collection<Flow> flows) {
        this(definitionId, TreePVector.from(flows));
    }

    private ActivateFlowsAction(String definitionId, PSequence<Flow> flows) {
        this.definitionId = definitionId;
        this.flows = flows;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public Collection<Flow> getFlows() {
        return flows;
    }

    public ActivateFlowsAction addFlow(String elementId, int count) {
        return new ActivateFlowsAction(definitionId, flows.plus(new Flow(elementId, count)));
    }

    public ActivateFlowsAction addFlows(List<String> elementIds, int count) {
        PSequence<Flow> flows = this.flows;
        for (String elementId : elementIds) {
            flows = flows.plus(new Flow(elementId, count));
        }
        return new ActivateFlowsAction(definitionId, flows);
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "ActivateFlowsAction [definitionId=" + definitionId + ", flows=" + flows + "]";
    }

    public static class Flow implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String elementId;
        private final int count;

        public Flow(String elementId, int count) {
            this.elementId = elementId;
            this.count = count;
        }

        public String getElementId() {
            return elementId;
        }

        public int getCount() {
            return count;
        }

        @Override
        @CoverageIgnore
        public String toString() {
            return "Flow [elementId=" + elementId + ", count=" + count + "]";
        }
    }
}
