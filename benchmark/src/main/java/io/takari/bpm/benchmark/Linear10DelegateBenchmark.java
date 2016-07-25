package io.takari.bpm.benchmark;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import io.takari.bpm.task.ServiceTaskResolver;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Arrays;

/**
 * Linear process with 10 delegate tasks. The delegate task is an instance of
 * {@link JavaDelegate} interface. It is called by resolving its name with
 * {@link ServiceTaskResolver}.
 */
public class Linear10DelegateBenchmark {

    private static final String EXPR = "${t}";

    @State(Scope.Benchmark)
    public static class BenchmarkState extends AbstractBenchmarkState {

        public BenchmarkState() {
            super(true, false, new ProcessDefinition("test", Arrays.asList(
                    new StartEvent("start"),
                    new SequenceFlow("f1", "start", "t1"),
                    new ServiceTask("t1", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f2", "t1", "t2"),
                    new ServiceTask("t2", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f3", "t2", "t3"),
                    new ServiceTask("t3", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f4", "t3", "t4"),
                    new ServiceTask("t4", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f5", "t4", "t5"),
                    new ServiceTask("t5", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f6", "t5", "t6"),
                    new ServiceTask("t6", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f7", "t6", "t7"),
                    new ServiceTask("t7", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f8", "t7", "t8"),
                    new ServiceTask("t8", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f9", "t8", "t9"),
                    new ServiceTask("t9", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f10", "t9", "t10"),
                    new ServiceTask("t10", ExpressionType.DELEGATE, EXPR),
                    new SequenceFlow("f11", "t10", "end"),
                    new EndEvent("end"))));
            
            getServiceTaskRegistry().register("t", new JavaDelegate() {
                @Override
                public void execute(ExecutionContext ctx) throws Exception {
                    // NOOP
                }
            });
        }
    }

    @Benchmark
    public void test(BenchmarkState state) {
        try {
            state.getEngine().start("a", "test", null);
        } catch (ExecutionException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Linear10DelegateBenchmark.class.getSimpleName())
                .forks(0)
                .warmupIterations(15)
                .measurementIterations(15)
                .threads(4)
                .build();

        new Runner(opt).run();
    }
}
