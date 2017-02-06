package io.takari.bpm.context;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.actions.UnsetVariableAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.state.Variables;

import java.util.*;

public class ExecutionContextImpl implements ExecutionContext {

    private final ExpressionManager exprManager;
    private final Variables source;
    private final Map<String, Change> changes = new HashMap<>();

    public ExecutionContextImpl(ExpressionManager exprManager, Variables source) {
        this.exprManager = exprManager;
        this.source = source;
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
}
