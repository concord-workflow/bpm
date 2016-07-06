package io.takari.bpm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexedProcessDefinition extends ProcessDefinition {

    private final Map<String, List<SequenceFlow>> outgoingFlows;

    public IndexedProcessDefinition(ProcessDefinition pd) {
        super(pd.getId(), pd.getChildren());
        setName(pd.getName());
        this.outgoingFlows = indexOutgoingFlows();
    }

    public List<SequenceFlow> findOptionalOutgoingFlows(String from) {
        return outgoingFlows.get(from);
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
}
