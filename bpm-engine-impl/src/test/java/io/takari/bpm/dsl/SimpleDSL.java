package io.takari.bpm.dsl;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.el.DefaultExpressionManager;
import io.takari.bpm.model.*;
import io.takari.bpm.task.ServiceTaskRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class SimpleDSL {

    private static final Logger log = LoggerFactory.getLogger(SimpleDSL.class);

    public static Map<String, ProcessDefinition> from(Flow ... flows) {
        Map<String, ProcessDefinition> m = new HashMap<String, ProcessDefinition>();
        for (Flow f : flows) {
            ProcessDefinition pd = from(f);
            m.put(pd.getId(), pd);
        }
        return m;
    }

    private static ProcessDefinition from(Flow f) {
        AtomicLong counter = new AtomicLong();

        List<AbstractElement> children = new ArrayList<>();

        String startId = "start" + counter.incrementAndGet();
        children.add(new StartEvent(startId));

        String lastId = startId;

        for (Step s : f.getSteps()) {
            String fInId = "fIn" + counter.incrementAndGet();
            String stepId = "step" + counter.incrementAndGet();
            children.add(new SequenceFlow(fInId, lastId, stepId));
            children.add(convert(s, stepId, counter));
            lastId = stepId;
        }

        String fOutId = "fOut" + counter.incrementAndGet();
        String endId = "end" + counter.incrementAndGet();
        children.add(new SequenceFlow(fOutId, lastId, endId));
        children.add(new EndEvent(endId));

        return new ProcessDefinition(f.getId(), children);
    }

    private static AbstractElement convert(Step s, String stepId, AtomicLong counter) {
        if (s instanceof TaskStep) {
            return task((TaskStep) s, stepId, counter);
        } else if (s instanceof CallStep) {
            return call((CallStep)s, stepId);
        } else if (s instanceof ParallelStep) {
            return parallel((ParallelStep) s, stepId, counter);
        }

        throw new IllegalArgumentException("Unsupported step type: " + s.getClass());
    }

    private static SubProcess task(TaskStep step, String stepId, AtomicLong counter) {
        List<AbstractElement> children = new ArrayList<>();
        String startId = "subStart" + counter.incrementAndGet();
        String f1Id = "f" + counter.incrementAndGet();
        String taskId = "t" + counter.incrementAndGet();
        String f2Id = "f" + counter.incrementAndGet();
        String f3Id = "f" + counter.incrementAndGet();
        String onSuccessId = "ok_" + step.getTask();
        String subId = "sub" + counter.incrementAndGet();
        String end1Id = "subEnd" + counter.incrementAndGet();
        String onFailureId = "fail_" + step.getTask();

        children.add(new StartEvent(startId));
        children.add(new SequenceFlow(f1Id, startId, taskId));
        children.add(makeTask(step, taskId, onSuccessId, onFailureId));
        children.add(new SequenceFlow(f2Id, taskId, subId));
        children.add(wrapEvents(subId, onSuccessId, onFailureId, counter));
        children.add(new SequenceFlow(f3Id, subId, end1Id));
        children.add(new EndEvent(end1Id));

        return new SubProcess(stepId, children);
    }

    private static ServiceTask makeTask(TaskStep step, String taskId, String onSuccess, String onFailure) {
        String expr = String.format("${dsl.task(execution, '%s', '%s', '%s')}",
                DefaultExpressionManager.quote(step.getTask()),
                DefaultExpressionManager.quote(onSuccess),
                DefaultExpressionManager.quote(onFailure));

        return new ServiceTask(taskId, ExpressionType.SIMPLE, expr);
    }

    public static void registerDslTask(ServiceTaskRegistryImpl registry) {
        registry.register("dsl", new DslTask() {
            @Override
            public void task(ExecutionContext ctx, String task, String onSuccess, String onFailure) {
                log.info("task ['{}', '{}', '{}'] -> done", task, onSuccess, onFailure);
            }
        });
    }

    private static CallActivity call(CallStep step, String stepId) {
        return new CallActivity(stepId, step.getCall());
    }

    private static AbstractElement wrapEvents(String subId, String onSuccessId, String onFailureId, AtomicLong counter) {
        List<AbstractElement> children = new ArrayList<>();

        String startId = "start" + counter.incrementAndGet();
        children.add(new StartEvent(startId));
        String f1Id = "f" + counter.incrementAndGet();
        String gwId = "gw" + counter.incrementAndGet();
        children.add(new SequenceFlow(f1Id, startId, gwId));
        children.add(new EventBasedGateway(gwId));

        String f2Id = "f" + counter.incrementAndGet();
        children.add(new SequenceFlow(f2Id, gwId, onSuccessId));
        children.add(new IntermediateCatchEvent(onSuccessId, onSuccessId));
        String f3id = "f" + counter.incrementAndGet();
        String endId = "end" + counter.incrementAndGet();
        children.add(new SequenceFlow(f3id, onSuccessId, endId));
        children.add(new EndEvent(endId));

        String f4id = "f" + counter.incrementAndGet();
        children.add(new SequenceFlow(f4id, gwId, onFailureId));
        children.add(new IntermediateCatchEvent(onFailureId, onFailureId));
        String f5id = "f" + counter.incrementAndGet();
        String errorEndId = "errorEnd" + counter.incrementAndGet();
        children.add(new SequenceFlow(f5id, onFailureId, errorEndId));
        children.add(new EndEvent(errorEndId, onFailureId));

        return new SubProcess(subId, children);
//        return new IntermediateCatchEvent(subId, onSuccessId);
    }

    private static SubProcess parallel(ParallelStep step, String stepId, AtomicLong counter) {
        List<AbstractElement> children = new ArrayList<>();

        String startId = "parStart" + counter.incrementAndGet();
        children.add(new StartEvent(startId));
        String lastId = startId;

        List<AbstractElement> parallel = new ArrayList<>();
        for (Step s : step.getSteps()) {
            String sId = "step" + counter.incrementAndGet();
            parallel.add(convert(s, sId, counter));
        }

        String gw1Id = "gw" + counter.incrementAndGet();
        String gw2Id = "gw" + counter.incrementAndGet();
        String f1Id = "f" + counter.incrementAndGet();
        children.add(new SequenceFlow(f1Id, lastId, gw1Id));
        children.add(new InclusiveGateway(gw1Id));

        for (AbstractElement e : parallel) {
            String fStartId = "f" + counter.incrementAndGet();
            children.add(new SequenceFlow(fStartId, gw1Id, e.getId()));
            children.add(e);
            String fEndId = "f" + counter.incrementAndGet();
            children.add(new SequenceFlow(fEndId, e.getId(), gw2Id));
        }

        children.add(new InclusiveGateway(gw2Id));

        String f2Id = "f" + counter.incrementAndGet();
        String endId = "parEnd" + counter.incrementAndGet();
        children.add(new SequenceFlow(f2Id, gw2Id, endId));
        children.add(new EndEvent(endId));

        return new SubProcess(stepId, children);
    }

    public interface DslTask {

        void task(ExecutionContext ctx, String task, String onSuccess, String onFailure);
    }

    private SimpleDSL() {
    }
}
