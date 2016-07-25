package io.takari.bpm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;

public class IndexedProcessDefinition extends ProcessDefinition {
	
	private static final long serialVersionUID = 1L;

    private final Map<String, List<SequenceFlow>> outgoingFlows;
    private final Map<String, List<BoundaryEvent>> boundaryEvents;

    public IndexedProcessDefinition(ProcessDefinition pd) {
        super(pd.getId(), pd.getChildren());
        setName(pd.getName());
        this.outgoingFlows = indexOutgoingFlows();
        this.boundaryEvents = indexBoundaryEvents();
    }

    public List<SequenceFlow> findOptionalOutgoingFlows(String from) {
        return outgoingFlows.get(from);
    }

    public List<BoundaryEvent> findOptionalBoundaryEvents(String attachedToRef) {
        return boundaryEvents.get(attachedToRef);
    }

    private Map<String, List<SequenceFlow>> indexOutgoingFlows() {
        Map<String, List<SequenceFlow>> m = new HashMap<>();
        indexOutgoingFlows0(this, m);
        return ImmutableMap.copyOf(m);
    }

    private static void indexOutgoingFlows0(ProcessDefinition pd, Map<String, List<SequenceFlow>> accumulator) {
        for (AbstractElement e : pd.getChildren()) {
            String id = e.getId();
            List<SequenceFlow> l = findOutgoingFlows(pd, id);
            accumulator.put(id, ImmutableList.copyOf(l));

            if (e instanceof ProcessDefinition) {
                indexOutgoingFlows0((ProcessDefinition) e, accumulator);
            }
        }
    }

    private static List<SequenceFlow> findOutgoingFlows(ProcessDefinition pd, String from) {
        List<SequenceFlow> result = new ArrayList<>();

        for (AbstractElement e : pd.getChildren()) {
            if (e instanceof SequenceFlow) {
                SequenceFlow f = (SequenceFlow) e;
                if (from.equals(f.getFrom())) {
                    result.add(f);
                }
            }
        }

        return result;
    }

    private Map<String, List<BoundaryEvent>> indexBoundaryEvents() {
        Map<String, List<BoundaryEvent>> m = new HashMap<>();
        indexBoundaryEvents0(this, m);
        return m;
    }

    private static void indexBoundaryEvents0(ProcessDefinition pd, Map<String, List<BoundaryEvent>> accumulator) {
        for (AbstractElement e : pd.getChildren()) {
            if (e instanceof ProcessDefinition) {
                indexBoundaryEvents0((ProcessDefinition) e, accumulator);
            }

            if (!(e instanceof BoundaryEvent)) {
                continue;
            }

            BoundaryEvent ev = (BoundaryEvent) e;
            if (ev.getAttachedToRef() == null) {
                continue;
            }

            List<BoundaryEvent> l = accumulator.get(ev.getAttachedToRef());
            if (l == null) {
                l = new ArrayList<>();
                accumulator.put(ev.getAttachedToRef(), l);
            }
            l.add(ev);
        }
    }
}
