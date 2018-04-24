package io.takari.bpm;

import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.context.ExecutionContextFactory;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.reducers.*;
import io.takari.bpm.resource.ResourceResolver;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.task.JavaDelegateHandler;
import io.takari.bpm.task.ServiceTaskRegistry;
import io.takari.bpm.task.UserTaskHandler;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class DefaultExecutor implements Executor {

    private final Reducer reducer;

    public DefaultExecutor(Configuration cfg,
                           ExecutionContextFactory<? extends ExecutionContextImpl> contextFactory,
                           ExecutorService executor,
                           ExecutionInterceptorHolder interceptors,
                           IndexedProcessDefinitionProvider definitionProvider,
                           UuidGenerator uuidGenerator,
                           EventPersistenceManager eventManager,
                           PersistenceManager persistenceManager,
                           JavaDelegateHandler javaDelegateHandler,
                           UserTaskHandler userTaskHandler,
                           ResourceResolver resourceResolver,
                           ServiceTaskRegistry taskRegistry) {

        this.reducer = new CombiningReducer(
                new ForkReducer(contextFactory),
                new CommandStackReducer(),
                new StatusReducer(),
                new FlowsReducer(),
                new VariablesReducer(contextFactory),
                new RaiseErrorReducer(contextFactory),
                new ExpressionsReducer(contextFactory, cfg, javaDelegateHandler, executor),
                new InterceptorEventsReducer(interceptors),
                new CallActivityReducer(definitionProvider, cfg),
                new EventsReducer(contextFactory, uuidGenerator, eventManager),
                new PersistenceReducer(persistenceManager),
                new EvaluatedFlowsReducer(contextFactory),
                new ActivationsReducer(interceptors),
                new FlowListenerReducer(contextFactory),
                new ScopeReducer(uuidGenerator),
                new EventGatewayReducer(),
                new UserTaskReducer(userTaskHandler),
                new ScriptReducer(contextFactory, cfg, resourceResolver, taskRegistry),
                new TerminateEventReducer(eventManager));
    }

    @Override
    public ProcessInstance eval(ProcessInstance instance, List<Action> actions) throws ExecutionException {
        for (Action a : actions) {
            instance = reducer.reduce(instance, a);
        }

        return instance;
    }
}
