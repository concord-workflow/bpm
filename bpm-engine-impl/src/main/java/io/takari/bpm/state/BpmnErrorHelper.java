package io.takari.bpm.state;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.RaiseErrorAction;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.actions.UnsetVariableAction;
import io.takari.bpm.api.BpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BpmnErrorHelper {

    private static final Logger log = LoggerFactory.getLogger(BpmnErrorHelper.class);

    private static final String KEY = "__bpmn_raised_error";
    private static final String DEFAULT_ERROR_REF = "__default_error_ref";

    public static BpmnError getRaisedError(Variables vars) {
        return (BpmnError) vars.getVariable(KEY);
    }

    public static Action raiseError(String definitionId, String elementId, String errorRef, Throwable cause) {
        String e = errorRef;
        if (e == null) {
            log.warn("raiseError ['{}', '{}', '{}'] -> empty error reference will be replaced with a default value", definitionId, elementId, errorRef);
            e = DEFAULT_ERROR_REF;
        }
        return raiseError(new BpmnError(definitionId, elementId, e, cause));
    }

    public static Action raiseErrorDeferred(String definitionId, String elementId, String errorRef, String causeExpression) {
        if (causeExpression == null) {
            return raiseError(definitionId, elementId, errorRef, null);
        }
        return new RaiseErrorAction(definitionId, elementId, errorRef, causeExpression);
    }

    public static Action raiseError(BpmnError error) {
        return new SetVariableAction(KEY, error);
    }

    public static Action clear() {
        return new UnsetVariableAction(KEY);
    }

    private BpmnErrorHelper() {
    }
}
