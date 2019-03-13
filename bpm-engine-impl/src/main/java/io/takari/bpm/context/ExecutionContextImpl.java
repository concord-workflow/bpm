package io.takari.bpm.context;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.actions.UnsetVariableAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.api.Variables;
import io.takari.bpm.el.ExpressionManager;

import java.util.*;

public class ExecutionContextImpl implements ExecutionContext {

    private final ExecutionContextFactory<? extends ExecutionContext> executionContextFactory;
    private final ExpressionManager exprManager;
    private final Variables source;
    private final Map<String, Change> changes = new HashMap<>();

    private String suspendMessageRef = null;
    private Object suspendPayload = null;
    private boolean resumeFromSameStep;

    private final String processDefinitionId;
    private final String elementId;

    public ExecutionContextImpl(ExecutionContextFactory<? extends ExecutionContext> executionContextFactory,
                                ExpressionManager exprManager, Variables source) {
        this(executionContextFactory, exprManager, source, null, null);
    }

    public ExecutionContextImpl(ExecutionContextFactory<? extends ExecutionContext> executionContextFactory,
                                ExpressionManager exprManager, Variables source, String processDefinitionId, String elementId) {
        this.executionContextFactory = executionContextFactory;
        this.exprManager = exprManager;
        this.source = source;
        this.processDefinitionId = processDefinitionId;
        this.elementId = elementId;
    }

    public <T> T eval(String expr, Class<T> type) {
        return exprManager.eval(this, expr, type);
    }

    @Override
    public Object getVariable(String key) {
        return currentValue(key);
    }

    @Override
    public Map<String, Object> getVariables() {
        return source.asMap();
    }

    @Override
    public void setVariable(String key, Object value) {
        changes.put(key, new Change(ChangeType.SET, value));
    }

    @Override
    public boolean hasVariable(String key) {
        Change c = changes.get(key);
        if (c != null && c.getType() == ChangeType.SET) {
            return true;
        }

        return source.hasVariable(key);
    }

    @Override
    public void removeVariable(String key) {
        changes.put(key, new Change(ChangeType.UNSET, null));
    }

    @Override
    public Set<String> getVariableNames() {
        return source.getVariableNames();
    }

    private Object currentValue(String key) {
        Change c = changes.get(key);
        if (c != null) {
            switch (c.getType()) {
                case SET: {
                    return c.getValue();
                }
                case UNSET: {
                    return null;
                }
            }
        }

        return source.getVariable(key);
    }

    public List<Action> toActions() {
        List<Action> actions = new ArrayList<>();

        for (Map.Entry<String, Change> e : changes.entrySet()) {
            String key = e.getKey();
            Change c = e.getValue();

            if (c.getType() == ChangeType.SET) {
                actions.add(new SetVariableAction(key, c.getValue()));
            } else if (c.getType() == ChangeType.UNSET) {
                actions.add(new UnsetVariableAction(key));
            }
        }

        return actions;
    }

    public Variables toVariables() {
        Variables dst = source;

        for (Map.Entry<String, Change> e : changes.entrySet()) {
            String key = e.getKey();
            Change c = e.getValue();

            if (c.getType() == ChangeType.SET) {
                dst = dst.setVariable(key, c.getValue());
            } else if (c.getType() == ChangeType.UNSET) {
                dst = dst.removeVariable(key);
            }
        }

        return dst;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();

        for (Variables v : stack(source)) {
            m.putAll(v.asMap());
        }

        for (Map.Entry<String, Change> e : changes.entrySet()) {
            String k = e.getKey();
            Change c = e.getValue();

            if (c.getType() == ChangeType.SET) {
                m.put(k, c.getValue());
            } else if (c.getType() == ChangeType.UNSET) {
                m.remove(k);
            }
        }

        return m;
    }

    @Override
    public Object interpolate(Object v) {
        return exprManager.interpolate(executionContextFactory, this, v);
    }

    @Override
    public void suspend(String messageRef) {
        this.suspendMessageRef = messageRef;
    }

    @Override
    public void suspend(String messageRef, Object payload, boolean resumeFromSameStep) {
        this.suspendMessageRef = messageRef;
        this.suspendPayload = payload;
        this.resumeFromSameStep = resumeFromSameStep;
    }

    public String getSuspendMessageRef() {
        return suspendMessageRef;
    }

    public Object getSuspendPayload() {
        return suspendPayload;
    }

    public boolean isResumeFromSameStep() {
        return resumeFromSameStep;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    private static List<Variables> stack(Variables tail) {
        List<Variables> l = new ArrayList<>();

        Variables v = tail;
        while (true) {
            if (v == null) {
                break;
            }

            l.add(v);

            v = v.getParent();
        }
        Collections.reverse(l);

        return l;
    }
}
