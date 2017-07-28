package io.takari.bpm.reducers;

import io.takari.bpm.Configuration;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ExecuteScriptAction;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.api.BpmnError;
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
import io.takari.bpm.task.ServiceTaskRegistry;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;

@Impure
public class ScriptReducer extends BpmnErrorHandlingReducer {

    private final ResourceResolver resourceResolver;
    private final ExpressionManager expressionManager;
    private final ServiceTaskRegistry taskRegistry;
    private final ScriptEngineManager scriptEngineManager;

    public ScriptReducer(Configuration cfg, ResourceResolver resourceResolver,
                         ExpressionManager expressionManager, ServiceTaskRegistry taskRegistry) {

        super(cfg);
        this.resourceResolver = resourceResolver;
        this.expressionManager = expressionManager;
        this.taskRegistry = taskRegistry;
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

        ScriptEngine engine = getEngine(t);
        if (engine == null) {
            throw new ExecutionException("Script engine not found: " + t.getLanguage());
        }

        Variables vars = VariablesHelper.applyInVariables(expressionManager, state.getVariables(), t.getIn(), t.isCopyAllVariables());
        ExecutionContextImpl ctx = new ExecutionContextImpl(expressionManager, vars);

        // expose all available variables plus the context
        Bindings b = engine.createBindings();
        b.put("execution", ctx);
        b.put("tasks", new TaskAccessor(taskRegistry));
        b.putAll(ctx.toMap());

        try (Reader input = openReader(t)) {
            engine.eval(input, b);

            // continue the process execution
            state = StateHelper.push(state, new FollowFlowsAction(a.getDefinitionId(), a.getElementId()));
        } catch (BpmnError e) {
            state = handleBpmnError(state, a, e);
        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            state = handleException(state, a, e);
        }

        // apply the changes before continuing the execution
        state = VariablesHelper.applyOutVariables(expressionManager, state, ctx, t.getOut());

        return state;
    }

    private ScriptEngine getEngine(ScriptTask t) throws ExecutionException {
        if (t.getLanguage() != null) {
            return scriptEngineManager.getEngineByName(t.getLanguage());
        }

        if (t.getContent() == null) {
            throw new ExecutionException("Script task must have a language set or a path to an external script: " + t.getId());
        }

        String ext = getExtension(t.getContent());
        if (ext == null) {
            throw new ExecutionException("Unknown external script extension: " + t.getContent());
        }

        return scriptEngineManager.getEngineByExtension(ext);
    }

    private Reader openReader(ScriptTask t) throws ExecutionException {
        Type type = t.getType();

        if (type == Type.REFERENCE) {
            String ref = t.getContent();

            try {
                InputStream in = resourceResolver.getResourceAsStream(ref);
                if (in == null) {
                    throw new ExecutionException("Resource not found: " + ref);
                }

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

    private ProcessInstance handleException(ProcessInstance state, ExecuteScriptAction a, Exception e) throws ExecutionException {
        return handleException(state, a.getDefinitionId(), a.getElementId(), e, null, null);
    }

    private ProcessInstance handleBpmnError(ProcessInstance state, ExecuteScriptAction a, BpmnError e) throws ExecutionException {
        return handleBpmnError(state, a.getDefinitionId(), a.getElementId(), e, null, null);
    }

    private static String getExtension(String s) {
        if (s == null) {
            return null;
        }

        int i = s.lastIndexOf(".");
        if (i < 0 || i + 1 >= s.length()) {
            return null;
        }

        return s.substring(i + 1);
    }

    public static class TaskAccessor {

        private final ServiceTaskRegistry taskRegistry;

        public TaskAccessor(ServiceTaskRegistry taskRegistry) {
            this.taskRegistry = taskRegistry;
        }

        public Object get(String key) {
            return taskRegistry.getByKey(key);
        }
    }
}
