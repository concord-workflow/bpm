package io.takari.bpm.reducers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ProcessFlowListenersAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.ExecutionListener;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.ProcessInstance;

@Impure
public class FlowListenerReducer implements Reducer {
    
    private static final Logger log = LoggerFactory.getLogger(FlowListenerReducer.class);

    private final ExpressionManager expressionManager;
    
    public FlowListenerReducer(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof ProcessFlowListenersAction)) {
            return state;
        }
        
        ProcessFlowListenersAction a = (ProcessFlowListenersAction) action;
        
        IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
        SequenceFlow f = (SequenceFlow) ProcessDefinitionUtils.findElement(pd, a.getElementId());
        
        ExecutionContextImpl ctx = new ExecutionContextImpl(expressionManager, state.getVariables());
        processListeners(expressionManager, ctx, f);
        
        // expression evaluation may have side-effects, but they are ignored
        // there
        if (!ctx.toActions().isEmpty()) {
            log.warn("reduce ['{}', '{}'] -> variables changes in the execution context will be ignored",
                    state.getBusinessKey(), a.getElementId());
        }
        
        return state;
    }
    


    /**
     * Handle process flow listeners. Listener reference can be specified with
     * EL expression.
     * @param ctx current execution context.
     * @param f processing flow.
     * @throws ExecutionException
     */
    private static void processListeners(ExpressionManager em, ExecutionContext ctx, SequenceFlow f) throws ExecutionException {
        if (f.getListeners() == null) {
            return;
        }

        for (SequenceFlow.ExecutionListener l : f.getListeners()) {
            ExpressionType type = l.getType();
            String expr = l.getExpression();
            if (expr == null) {
                continue;
            }

            try {
                switch (type) {
                    case SIMPLE: {
                        em.eval(ctx, expr, Object.class);
                        break;
                    }

                    case DELEGATE: {
                        ExecutionListener d = em.eval(ctx, expr, ExecutionListener.class);
                        d.notify(ctx);
                        break;
                    }
                    
                    case NONE: {
                        // NOOP
                    }
                }
            } catch (Exception e) {
                log.error("processListeners ['{}'] -> error", f.getId(), e);
                throw new ExecutionException("Unhandled listener exception: " + e.getMessage(), e);
            }
        }
    }
}
