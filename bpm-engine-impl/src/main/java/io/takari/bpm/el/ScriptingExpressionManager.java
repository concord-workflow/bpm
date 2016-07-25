package io.takari.bpm.el;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.task.KeyAwareServiceTaskRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

public class ScriptingExpressionManager implements ExpressionManager {

    private static final Logger log = LoggerFactory.getLogger(ScriptingExpressionManager.class);

    private final ThreadLocal<ScriptEngine> engine;
    private final KeyAwareServiceTaskRegistry taskRegistry;

    public ScriptingExpressionManager(final String engineName, KeyAwareServiceTaskRegistry taskRegistry) {
        this.engine = new ThreadLocal<ScriptEngine>() {
            @Override
            protected ScriptEngine initialValue() {
                return new ScriptEngineManager().getEngineByName(engineName);
            }
        };

        this.taskRegistry = taskRegistry;
    }

    @Override
    public <T> T eval(ExecutionContext ctx, String expr, Class<T> type) {
        try {
            ScriptEngine e = engine.get();
            Bindings b = new CustomBindings(taskRegistry, ctx);
            return type.cast(e.eval(expr, b));
        } catch (Exception e) {
            log.error("eval ['{}'] -> error", expr, e);
            throw new RuntimeException(e);
        }
    }

    public static class CustomBindings extends SimpleBindings {

        private static final String CONTEXT_KEY = "execution";

        private final KeyAwareServiceTaskRegistry taskRegistry;

        public CustomBindings(KeyAwareServiceTaskRegistry taskRegistry, ExecutionContext ctx) {
            this.taskRegistry = taskRegistry;
            put(CONTEXT_KEY, ctx);
        }

        @Override
        public Object get(Object key) {
            if (super.containsKey(key)) {
                return super.get(key);
            }

            return taskRegistry.getByKey((String) key);
        }

        @Override
        public boolean containsKey(Object key) {
            return super.containsKey(key) || taskRegistry.containsKey((String) key);
        }
    }
}
