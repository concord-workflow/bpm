package io.takari.bpm.el;

import com.oracle.truffle.js.scriptengine.GraalJSEngineFactory;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.task.KeyAwareServiceTaskRegistry;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

public class ScriptingExpressionManager implements ExpressionManager {

    private static final Logger log = LoggerFactory.getLogger(ScriptingExpressionManager.class);

    private static final String CONTEXT_KEY = "execution";

    private final ThreadLocal<ScriptEngine> engine;
    private final KeyAwareServiceTaskRegistry taskRegistry;

    public ScriptingExpressionManager(final String engineName, KeyAwareServiceTaskRegistry taskRegistry) {
        this.engine = ThreadLocal.withInitial(() -> createEngine(engineName));
        this.taskRegistry = taskRegistry;
    }

    private static ScriptEngine createEngine(String engineName) {
        if (new GraalJSEngineFactory().getNames().contains(engineName)) {
            return GraalJSScriptEngine.create(
                    Engine.newBuilder()
                            .option("engine.WarnInterpreterOnly", "false")
                            .build(),
                    Context.newBuilder("js")
                            .allowHostAccess(HostAccess.ALL));
        }
        return new ScriptEngineManager().getEngineByName(engineName);
    }

    @Override
    public <T> T eval(ExecutionContext ctx, String expr, Class<T> type) {
        try {
            ScriptEngine e = engine.get();
            if (e instanceof GraalJSScriptEngine graalEngine) {
                return evalGraalJs(graalEngine, ctx, expr, type);
            }
            Bindings b = new CustomBindings(taskRegistry, ctx);
            return type.cast(e.eval(expr, b));
        } catch (Exception e) {
            log.error("eval ['{}'] -> error", expr, e);
            throw new RuntimeException(e);
        }
    }

    private <T> T evalGraalJs(GraalJSScriptEngine graalEngine, ExecutionContext ctx, String expr, Class<T> type) {
        Context polyglotContext = graalEngine.getPolyglotContext();
        Value fn = polyglotContext.eval("js",
                "(function(__bpm_bindings, __bpm_eval_expr) { with (__bpm_bindings) { return eval(__bpm_eval_expr); } })");
        Value result = fn.execute(new CustomProxyObject(taskRegistry, ctx), expr);
        if (result == null || result.isNull()) {
            return null;
        }
        if (result.isHostObject()) {
            return type.cast(result.asHostObject());
        }
        return type.cast(result.as(Object.class));
    }

    private static final class CustomProxyObject implements ProxyObject {

        private final KeyAwareServiceTaskRegistry taskRegistry;
        private final ExecutionContext ctx;

        private CustomProxyObject(KeyAwareServiceTaskRegistry taskRegistry, ExecutionContext ctx) {
            this.taskRegistry = taskRegistry;
            this.ctx = ctx;
        }

        @Override
        public Object getMember(String key) {
            if (CONTEXT_KEY.equals(key)) {
                return ctx;
            }
            if (ctx.hasVariable(key)) {
                return ctx.getVariable(key);
            }
            return taskRegistry.getByKey(key);
        }

        @Override
        public Object getMemberKeys() {
            return new String[0];
        }

        @Override
        public boolean hasMember(String key) {
            return CONTEXT_KEY.equals(key) || ctx.hasVariable(key) || taskRegistry.containsKey(key);
        }

        @Override
        public void putMember(String key, Value value) {
            ctx.setVariable(key, value == null || value.isNull() ? null : value.as(Object.class));
        }
    }

    public static class CustomBindings extends SimpleBindings {

        private final KeyAwareServiceTaskRegistry taskRegistry;
        private final ExecutionContext ctx;

        public CustomBindings(KeyAwareServiceTaskRegistry taskRegistry, ExecutionContext ctx) {
            this.taskRegistry = taskRegistry;
            this.ctx = ctx;
            put(CONTEXT_KEY, ctx);
        }

        @Override
        public Object get(Object key) {
            if (super.containsKey(key)) {
                return super.get(key);
            }

            Object res = ctx.getVariable((String) key);
            if (res != null) {
                return res;
            }

            return taskRegistry.getByKey((String) key);
        }

        @Override
        public boolean containsKey(Object key) {
            return super.containsKey(key) || ctx.hasVariable((String) key) || taskRegistry.containsKey((String) key);
        }
    }
}
