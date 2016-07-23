package io.takari.bpm.state;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.actions.UnsetVariableAction;

public final class BpmnErrorHelper {

    private static final String KEY = "__bpmn_raised_error";

    public static String getRaisedError(Variables vars) {
        return (String) vars.getVariable(KEY);
    }

    public static Action raiseError(String errorRef) {
        return new SetVariableAction(KEY, errorRef);
    }

    public static Action clear() {
        return new UnsetVariableAction(KEY);
    }

    private BpmnErrorHelper() {
    }
}
