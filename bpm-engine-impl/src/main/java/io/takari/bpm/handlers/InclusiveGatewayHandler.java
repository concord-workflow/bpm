package io.takari.bpm.handlers;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.SequenceFlow;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InclusiveGatewayHandler extends ParallelGatewayHandler {
    
    private static final Logger log = LoggerFactory.getLogger(InclusiveGatewayHandler.class);

    public InclusiveGatewayHandler(AbstractEngine engine) {
        super(engine);
    }

    @Override
    protected void processInactive(DefaultExecution s, ProcessElementCommand c, List<SequenceFlow> inactive) throws ExecutionException {
        if (inactive == null || inactive.isEmpty()) {
            return;
        }
        
        IndexedProcessDefinition pd = getProcessDefinition(c);
        String gwId = ProcessDefinitionUtils.findNextGatewayId(pd, c.getElementId());
        int count = inactive.size();
        log.debug("processInactive ['{}', '{}'] -> adding '{}' activations '{}' time(s)", s.getId(), c.getElementId(), gwId, count);
        s.inc(c.getProcessDefinitionId(), gwId, count);
    }
}
