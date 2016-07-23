package io.takari.bpm;

import java.util.Arrays;

import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.StartEvent;

public final class TestConstants {

    public static ProcessDefinition EMPTY_PROCESS = new ProcessDefinition("default", Arrays.asList(
                    new StartEvent("start"),
                    new SequenceFlow("f1", "start", "end"),
                    new EndEvent("end")));

    private TestConstants() {
    }
}
