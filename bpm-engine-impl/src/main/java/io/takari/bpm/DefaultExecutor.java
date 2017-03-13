package io.takari.bpm;

import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.reducers.*;
import io.takari.bpm.resource.ResourceResolver;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.task.UserTaskHandler;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class DefaultExecutor implements Executor {

    private final Reducer reducer;

    public DefaultExecutor(Configuration cfg,
                           ExpressionManager expressionManager,
                           ExecutorService executor,
                           ExecutionInterceptorHolder interceptors,
                           IndexedProcessDefinitionProvider definitionProvider,
                           UuidGenerator uuidGenerator,
                           EventPersistenceManager eventManager,
                           PersistenceManager persistenceManager,
                           UserTaskHandler userTaskHandler,
                           ResourceResolver resourceResolver) {

        this.reducer = new CombiningReducer(
                new ForkReducer(expressionManager),
                new CommandStackReducer(),
                new StatusReducer(),
                new FlowsReducer(),
                new VariablesReducer(expressionManager),
                new RaiseErrorReducer(expressionManager),
                new ExpressionsReducer(cfg, expressionManager, executor),
                new InterceptorEventsReducer(interceptors),
                new CallActivityReducer(definitionProvider, cfg),
                new EventsReducer(uuidGenerator, expressionManager, eventManager),
                new PersistenceReducer(persistenceManager),
                new EvaluatedFlowsReducer(expressionManager),
                new ActivationsReducer(interceptors),
                new FlowListenerReducer(expressionManager),
                new ScopeReducer(uuidGenerator),
                new EventGatewayReducer(),
                new UserTaskReducer(userTaskHandler),
                new ScriptReducer(resourceResolver, expressionManager));
    }

    @Override
    public ProcessInstance eval(ProcessInstance instance, List<Action> actions) throws ExecutionException {
        for (Action a : actions) {
            instance = reducer.reduce(instance, a);
        }

        return instance;
    }
}
