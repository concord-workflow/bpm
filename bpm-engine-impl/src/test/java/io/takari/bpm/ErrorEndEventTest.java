package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.fail;

public class ErrorEndEventTest extends AbstractEngineTest {

    @Test
    public void test() throws Exception {
        getConfiguration().setThrowExceptionOnErrorEnd(true);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end", "error!")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        try {
            getEngine().start(key, processId, null);
            fail("should throw an exception");
        } catch (ExecutionException e) {
        }
    }
}
