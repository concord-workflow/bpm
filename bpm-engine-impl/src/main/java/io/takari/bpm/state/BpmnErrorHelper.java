package io.takari.bpm.state;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.actions.UnsetVariableAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BpmnErrorHelper {

    private static final Logger log = LoggerFactory.getLogger(BpmnErrorHelper.class);

    private static final String KEY = "__bpmn_raised_error";
    private static final String DEFAULT_ERROR_REF = "__default_error_ref";

    public static String getRaisedError(Variables vars) {
        return (String) vars.getVariable(KEY);
    }

    public static Action raiseError(String errorRef) {
        String e = errorRef;
        if (e == null) {
            log.warn("raiseError ['{}'] -> empty error reference will be replaced with a default value", errorRef);
            e = DEFAULT_ERROR_REF;
        }
        return new SetVariableAction(KEY, e);
    }

    public static Action clear() {
        return new UnsetVariableAction(KEY);
    }

    private BpmnErrorHelper() {
    }
}
