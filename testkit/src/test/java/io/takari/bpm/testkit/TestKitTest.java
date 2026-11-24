package io.takari.bpm.testkit;

import io.takari.bpm.xml.activiti.ActivitiParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TestKitTest {

    @RegisterExtension
    final EngineExtension engine = new EngineExtension(new ActivitiParser());

    @Test
    @Deployment(resources = "test.bpmn")
    public void test() throws Exception {
        engine.startProcessInstanceByKey("test", null);
    }
}
