package io.takari.bpm.reducers;

import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ExecuteScriptAction;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.ScriptTask;
import io.takari.bpm.model.ScriptTask.Type;
import io.takari.bpm.resource.ResourceResolver;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.StateHelper;
import io.takari.bpm.state.Variables;
import io.takari.bpm.state.VariablesHelper;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;

@Impure
public class ScriptReducer implements Reducer {

    private final ResourceResolver resourceResolver;
    private final ExpressionManager expressionManager;
    private final ScriptEngineManager scriptEngineManager;

    public ScriptReducer(ResourceResolver resourceResolver, ExpressionManager expressionManager) {
        this.resourceResolver = resourceResolver;
        this.expressionManager = expressionManager;
        this.scriptEngineManager = new ScriptEngineManager();
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof ExecuteScriptAction)) {
            return state;
        }

        ExecuteScriptAction a = (ExecuteScriptAction) action;

        ProcessDefinition pd = state.getDefinition(a.getDefinitionId());
        ScriptTask t = (ScriptTask) ProcessDefinitionUtils.findElement(pd, a.getElementId());

        ScriptEngine engine = scriptEngineManager.getEngineByName(t.getLanguage());
        if (engine == null) {
            throw new ExecutionException("Script engine not found: " + t.getLanguage());
        }

        try (Reader input = openReader(t)) {
            Variables vars = VariablesHelper.applyInVariables(expressionManager, state.getVariables(), t.getIn());
            ExecutionContextImpl ctx = new ExecutionContextImpl(expressionManager, vars);

            Bindings b = engine.createBindings();
            b.put("execution", ctx);
            engine.eval(input, b);

            // continue the process execution
            state = StateHelper.push(state, new FollowFlowsAction(a.getDefinitionId(), a.getElementId()));

            // apply the changes before continuing the execution
            state = VariablesHelper.applyOutVariables(expressionManager, state, ctx, t.getOut());
        } catch (Exception e) {
            throw new ExecutionException("Error while executing a script", e);
        }

        return state;
    }

    private Reader openReader(ScriptTask t) throws ExecutionException {
        Type type = t.getType();

        if (type == Type.REFERENCE) {
            String ref = t.getContent();

            try {
                InputStream in = resourceResolver.getResourceAsStream(ref);
                return new InputStreamReader(in);
            } catch (IOException e) {
                throw new ExecutionException("Can't open resource: " + ref);
            }
        } else if (type == Type.CONTENT) {
            return new StringReader(t.getContent());
        } else {
            throw new ExecutionException("Unsupported script task type: " + type);
        }
    }
}
