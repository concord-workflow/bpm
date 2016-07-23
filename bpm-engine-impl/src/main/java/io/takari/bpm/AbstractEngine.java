package io.takari.bpm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FireOnFinishInterceptorsAction;
import io.takari.bpm.actions.FireOnResumeInterceptorsAction;
import io.takari.bpm.actions.FireOnSuspendInterceptorsAction;
import io.takari.bpm.api.Engine;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.NoEventFoundException;
import io.takari.bpm.api.interceptors.ExecutionInterceptor;
import io.takari.bpm.event.Event;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.lock.LockManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.planner.Planner;
import io.takari.bpm.state.EventMapHelper;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.ProcessStatus;
import io.takari.bpm.state.StateHelper;

public abstract class AbstractEngine implements Engine {

    private static final Logger log = LoggerFactory.getLogger(AbstractEngine.class);

    protected abstract IndexedProcessDefinitionProvider getProcessDefinitionProvider();

    protected abstract UuidGenerator getUuidGenerator();

    protected abstract Planner getPlanner();

    protected abstract Executor getExecutor();

    protected abstract PersistenceManager getPersistenceManager();

    protected abstract ExecutionInterceptorHolder getInterceptorHolder();

    protected abstract EventPersistenceManager getEventManager();

    protected abstract LockManager getLockManager();

    @Override
    public void start(String processBusinessKey, String processDefinitionId, Map<String, Object> variables) throws ExecutionException {
        IndexedProcessDefinitionProvider pdp = getProcessDefinitionProvider();
        IndexedProcessDefinition pd = pdp.getById(processDefinitionId);

        UuidGenerator idg = getUuidGenerator();
        UUID instanceId = idg.generate();

        ProcessInstance state = StateHelper.createInitialState(getExecutor(), instanceId, processBusinessKey, pd, variables);

        LockManager lm = getLockManager();
        lm.lock(processBusinessKey);

        try {
            runLockSafe(state);
        } catch (Exception e) {
            // TODO move to the executor?
            getInterceptorHolder().fireOnError(processBusinessKey, e);
            throw e;
        } finally {
            lm.unlock(processBusinessKey);
        }
    }

    @Override
    public void resume(String processBusinessKey, String eventName, Map<String, Object> variables) throws ExecutionException {
        LockManager lm = getLockManager();
        lm.lock(processBusinessKey);

        try {
            EventPersistenceManager em = getEventManager();
            Collection<Event> evs = em.find(processBusinessKey, eventName);

            if (evs == null || evs.isEmpty()) {
                throw new NoEventFoundException("No event '%s' found for process '%s'", eventName, processBusinessKey);
            } else if (evs.size() > 1) {
                StringBuilder b = new StringBuilder();
                for (Event e : evs) {
                    b.append(e).append(",");
                }
                throw new ExecutionException("Non-unique event name in process '%s': %s. Events: %s", processBusinessKey, eventName, b);
            }

            Event e = evs.iterator().next();
            resumeLockSafe(e, variables);
        } catch (Exception e) {
            // TODO move to the executor?
            getInterceptorHolder().fireOnError(processBusinessKey, e);
            throw e;
        } finally {
            lm.unlock(processBusinessKey);
        }
    }

    @Override
    public void resume(UUID eventId, Map<String, Object> variables) throws ExecutionException {
        EventPersistenceManager em = getEventManager();
        Event ev = em.get(eventId);
        if (ev == null) {
            throw new NoEventFoundException("No event '%s' found", eventId);
        }

        String businessKey = ev.getProcessBusinessKey();

        LockManager lm = getLockManager();
        lm.lock(businessKey);

        try {
            resumeLockSafe(ev, variables);
        } catch (Exception e) {
            // TODO move to the executor?
            getInterceptorHolder().fireOnError(businessKey, e);
            throw e;
        } finally {
            lm.unlock(businessKey);
        }
    }

    @Override
    public void addInterceptor(ExecutionInterceptor i) {
        getInterceptorHolder().addInterceptor(i);
    }

    private void resumeLockSafe(Event e, Map<String, Object> variables) throws ExecutionException {
        String businessKey = e.getProcessBusinessKey();
        String eventName = e.getName();

        UUID eid = e.getExecutionId();
        log.debug("resumeLockSafe ['{}', '{}'] -> got '{}'", businessKey, eventName, eid);

        EventPersistenceManager em = getEventManager();
        if (e.isExclusive()) {
            // exclusive event means that only one event from the group of
            // events can happen. The rest of the events must be removed.
            em.clearGroup(businessKey, e.getGroupId());
        } else {
            em.remove(e.getId());
        }

        PersistenceManager pm = getPersistenceManager();
        ProcessInstance state = pm.get(eid);
        if (state == null) {
            throw new ExecutionException("No execution '%s' found for the process '%s'", eid, businessKey);
        }

        // enable the execution
        state = state.setStatus(ProcessStatus.RUNNING);

        // apply the external variables
        state = StateHelper.applyVariables(state, variables);

        // fire the interceptors
        // TODO move to the planner?
        state = getExecutor().eval(state, Arrays.asList(new FireOnResumeInterceptorsAction()));

        // process event-to-command mappings (e.g. add next command of the flow
        // to the stack)
        if (!EventMapHelper.isEmpty(state)) {
            state = EventMapHelper.pushCommands(state, e.getDefinitionId(), e.getId());
            if (e.isExclusive()) {
                // if the event is exclusive for its group, we need to remove
                // the whole group. exclusive events usually declared by
                // an event based gateway
                state = EventMapHelper.clearGroup(state, e.getDefinitionId(), e.getGroupId());
            } else {
                state = EventMapHelper.remove(state, e.getDefinitionId(), e.getId());
            }
        } else if (StateHelper.isDone(state)) {
            throw new ExecutionException("No event mapping found in process '%s' or no commands in execution", eid);
        }

        runLockSafe(state);
    }

    private void runLockSafe(ProcessInstance state) throws ExecutionException {
        log.debug("runLockSafe ['{}'] -> started...", state.getBusinessKey());

        while (true) {
            if (state.getStatus() != ProcessStatus.RUNNING) {
                log.debug("runLockSafe ['{}'] -> process is not running anymore: {}", state.getBusinessKey(), state.getStatus());
                break;
            }

            List<Action> actions = getPlanner().eval(state);
            state = getExecutor().eval(state, actions);
        }

        // fire the interceptors
        // TODO move to the planner?
        ProcessStatus status = state.getStatus();
        if (status == ProcessStatus.SUSPENDED) {
            state = getExecutor().eval(state, Arrays.asList(new FireOnSuspendInterceptorsAction()));
        } else if (status == ProcessStatus.FINISHED) {
            state = getExecutor().eval(state, Arrays.asList(new FireOnFinishInterceptorsAction()));
        }

        log.debug("runLockSafe ['{}'] -> done", state.getBusinessKey());
    }
}
