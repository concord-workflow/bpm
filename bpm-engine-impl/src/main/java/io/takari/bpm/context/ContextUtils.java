package io.takari.bpm.context;

import io.takari.bpm.actions.CreateEventAction;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.state.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ContextUtils {

    private static final Logger log = LoggerFactory.getLogger(ContextUtils.class);

    /**
     * Handles the {@link io.takari.bpm.api.ExecutionContext#suspend(String, Object)} flag.
     * @param state current process state
     * @param ctx current context
     * @param definitionId current definition ID (typically, from an {@link io.takari.bpm.actions.Action})
     * @param elementId current element ID (typically, from an {@link io.takari.bpm.actions.Action}).
     *                  Both {@code definitionId} and {@code elementId} will be used as the continuation point
     *                  after the process is resumed
     * @param nextCommand command that should be executed if the process is <b>not</b> going to be suspended.
     * @return new process state
     */
    public static ProcessInstance handleSuspend(ProcessInstance state, ExecutionContextImpl ctx, String definitionId, String elementId, Command nextCommand) {
        CommandStack stack = state.getStack();

        String messageRef = ctx.getSuspendMessageRef();
        if (messageRef == null) {
            log.debug("handleSuspend ['{}'] -> next action is '{}'", state.getBusinessKey(), nextCommand);
            stack = stack.push(nextCommand);
        } else {
            log.debug("handleSuspend ['{}'] -> suspend is requested '{}'", state.getBusinessKey(), messageRef);
            stack = stack.push(new PerformActionsCommand(
                    new CreateEventAction(definitionId, elementId, messageRef, null, null, null, ctx.getSuspendPayload(),
                            ctx.isResumeFromSameStep(), changesToVariablesMap(ctx.getChanges()))));
        }

        return state.setStack(stack);
    }

    private static Map<String, Object> changesToVariablesMap(Map<String, Change> changes) {
        if (changes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Change> e : changes.entrySet()) {
            if (e.getValue().getType() == ChangeType.SET) {
                result.put(e.getKey(), e.getValue().getValue());
            }
        }
        return result;
    }

    private ContextUtils() {
    }
}
