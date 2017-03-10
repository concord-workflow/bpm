package io.takari.bpm.reducers;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ProcessUserTaskAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.task.UserTaskHandler;

@Impure
public class UserTaskReducer implements Reducer {

    private final UserTaskHandler taskHandler;

    public UserTaskReducer(UserTaskHandler taskHandler) {
        this.taskHandler = taskHandler;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof ProcessUserTaskAction)) {
            return state;
        }

        ProcessUserTaskAction a = (ProcessUserTaskAction) action;
        return taskHandler.handle(state, a.getDefinitionId(), a.getElementId());
    }
}
