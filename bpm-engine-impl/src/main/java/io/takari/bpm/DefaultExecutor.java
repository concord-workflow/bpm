package io.takari.bpm;

import java.util.List;
import java.util.concurrent.ExecutorService;

import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.reducers.ActivationsReducer;
import io.takari.bpm.reducers.CallActivityReducer;
import io.takari.bpm.reducers.CombiningReducer;
import io.takari.bpm.reducers.CommandStackReducer;
import io.takari.bpm.reducers.EvaluatedFlowsReducer;
import io.takari.bpm.reducers.EventsReducer;
import io.takari.bpm.reducers.ExpressionsReducer;
import io.takari.bpm.reducers.FlowGroupsReducer;
import io.takari.bpm.reducers.FlowListenerReducer;
import io.takari.bpm.reducers.FlowsReducer;
import io.takari.bpm.reducers.ForkReducer;
import io.takari.bpm.reducers.InterceptorEventsReducer;
import io.takari.bpm.reducers.PersistenceReducer;
import io.takari.bpm.reducers.RaiseErrorReducer;
import io.takari.bpm.reducers.Reducer;
import io.takari.bpm.reducers.StatusReducer;
import io.takari.bpm.reducers.VariablesReducer;
import io.takari.bpm.state.ProcessInstance;

public class DefaultExecutor implements Executor {

    private final Reducer reducer;

    public DefaultExecutor(ExpressionManager expressionManager, ExecutorService executor,
            ExecutionInterceptorHolder interceptors, IndexedProcessDefinitionProvider definitionProvider,
            UuidGenerator uuidGenerator, EventPersistenceManager eventManager,
            PersistenceManager persistenceManager) {

        this.reducer = new CombiningReducer(
                new ForkReducer(expressionManager),
                new CommandStackReducer(),
                new StatusReducer(),
                new FlowsReducer(),
                new VariablesReducer(expressionManager),
                new RaiseErrorReducer(expressionManager),
                new ExpressionsReducer(expressionManager, executor),
                new InterceptorEventsReducer(interceptors),
                new CallActivityReducer(definitionProvider),
                new EventsReducer(uuidGenerator, expressionManager, eventManager),
                new PersistenceReducer(persistenceManager),
                new FlowGroupsReducer(uuidGenerator),
                new EvaluatedFlowsReducer(expressionManager),
                new ActivationsReducer(),
                new FlowListenerReducer(expressionManager));
    }

    @Override
    public ProcessInstance eval(ProcessInstance instance, List<Action> actions) throws ExecutionException {
        for (Action a : actions) {
            instance = reducer.reduce(instance, a);
        }

        return instance;
    }
}
