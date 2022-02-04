package io.takari.bpm.reducers;

import com.oracle.truffle.js.scriptengine.GraalJSEngineFactory;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import io.takari.bpm.Configuration;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ExecuteScriptAction;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.api.*;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.context.ContextUtils;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.ScriptTask;
import io.takari.bpm.model.ScriptTask.Type;
import io.takari.bpm.resource.ResourceResolver;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.VariablesHelper;
import io.takari.bpm.task.ServiceTaskRegistry;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

@Impure
public class ScriptReducer extends BpmnErrorHandlingReducer {

    private final ExecutionContextFactory<? extends ExecutionContextImpl> contextFactory;
    private final ResourceResolver resourceResolver;
    private final ServiceTaskRegistry taskRegistry;
    private final ScriptEngineManager scriptEngineManager;

    public ScriptReducer(ExecutionContextFactory<? extends ExecutionContextImpl> contextFactory,
                         Configuration cfg,
                         ResourceResolver resourceResolver,
                         ServiceTaskRegistry taskRegistry) {

        super(cfg);

        this.contextFactory = contextFactory;
        this.resourceResolver = resourceResolver;
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

        ExecutionContextImpl ctx = null;

        try {
            Variables vars = VariablesHelper.applyInVariables(contextFactory, state.getVariables(), t.getIn(), t.isCopyAllVariables());
            ctx = contextFactory.create(vars, a.getDefinitionId(), a.getElementId());

            t = interpolateScriptRef(ctx, t);

            ScriptEngine engine = getEngine(t);
            if (engine == null) {
                throw new ExecutionException("Script engine not found: " + t.getLanguage());
            }

            // expose all available variables plus the context
            Bindings b = engine.createBindings();
            b.put("polyglot.js.allowAllAccess", true);
            b.put("execution", ctx);
            b.put("tasks", new TaskAccessor(taskRegistry));
            b.putAll(ctx.toMap());

            try (Reader input = openReader(t)) {
                engine.eval(input, b);
            }

            // continue the process execution
            Command next = new PerformActionsCommand(new FollowFlowsAction(a.getDefinitionId(), a.getElementId()));
            state = ContextUtils.handleSuspend(state, ctx, a.getDefinitionId(), a.getElementId(), next);
        } catch (BpmnError e) {
            state = handleBpmnError(state, a, e);
        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            state = handleException(state, a, e);
        }

        // apply the changes before continuing the execution
        state = VariablesHelper.applyOutVariables(contextFactory, state, ctx, t.getOut());

        return state;
    }

    private ScriptTask interpolateScriptRef(ExecutionContext ctx, ScriptTask t) {
        if (t.getType() != Type.REFERENCE) {
            return t;
        }

        String ref = (String) ctx.interpolate(t.getContent());
        return new ScriptTask(t.getId(), t.getType(), t.getLanguage(), ref, t.getIn(), t.getOut());
    }

    private ScriptEngine getEngine(ScriptTask t) throws ExecutionException {
        if (t.getLanguage() != null) {
            if (new GraalJSEngineFactory().getNames().contains(t.getLanguage())) {
                return initializeGraalVm();
            }

            return scriptEngineManager.getEngineByName(t.getLanguage());
        }

        if (t.getContent() == null) {
            throw new ExecutionException("Script task must have a language set or a path to an external script: " + t.getId());
        }

        String ext = getExtension(t.getContent());
        if (ext == null) {
            throw new ExecutionException("Unknown external script extension: " + t.getContent());
        }

        if (new GraalJSEngineFactory().getExtensions().contains(ext)) {
            return initializeGraalVm();
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
        return handleException(state, a.getDefinitionId(), a.getElementId(), e, a.getErrors(), a.getDefaultError());
    }

    private ProcessInstance handleBpmnError(ProcessInstance state, ExecuteScriptAction a, BpmnError e) throws ExecutionException {
        return handleBpmnError(state, a.getDefinitionId(), a.getElementId(), e, a.getErrors(), a.getDefaultError());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ScriptEngine initializeGraalVm() {
        // Javascript array is converted in Java to an empty map #214 (https://github.com/oracle/graaljs/issues/214)
        HostAccess access = HostAccess.newBuilder(HostAccess.ALL)
                .targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class))).build();

        return GraalJSScriptEngine.create(Engine.newBuilder()
                        .allowExperimentalOptions(true)
                        .option("engine.WarnInterpreterOnly", "false")
                        .option("js.nashorn-compat", "true")
                        .build(),
                org.graalvm.polyglot.Context.newBuilder("js")
                        .allowHostAccess(access));
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
