package io.takari.bpm;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import io.takari.bpm.persistence.InMemPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.state.ProcessInstance;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SerializationTest extends AbstractEngineTest {

    public SerializationTest() {
        super(spy(new InMemPersistenceManager()));
    }

    @Test
    public void testNonSerializableTasks() throws Exception {
        PersistenceManager pm = getPersistenceManager();
        doAnswer(i -> {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
                out.writeObject(i.getArguments()[0]);
            }
            return i.callRealMethod();
        }).when(pm).save(any(ProcessInstance.class));

        // ---

        getConfiguration().setStoreExpressionEvalResultsInContext(true);

        // ---

        MyTask t1 = spy(new MyTask());
        getServiceTaskRegistry().register("t1", t1);

        String processId = "test";
        deploy(new ProcessDefinition(processId,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "ev"),
                new IntermediateCatchEvent("ev", "ev"),
                new SequenceFlow("f2", "ev", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                new SequenceFlow("f3", "t1", "end"),
                new EndEvent("end")
        ));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        getEngine().resume(key, "ev", null);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }

    public static class MyTask implements JavaDelegate {

        private final InputStream nonSerializableStuff = new ByteArrayInputStream(new byte[]{0, 1, 2, 3});

        @Override
        public void execute(ExecutionContext ctx) throws Exception {
            System.out.println(nonSerializableStuff.read());
        }
    }
}
