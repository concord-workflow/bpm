package io.takari.bpm.testkit;

import io.takari.bpm.xml.activiti.ActivitiParser;
import org.junit.Rule;
import org.junit.Test;

public class TestKitTest {

    @Rule
    public final EngineRule engineRule = new EngineRule(new ActivitiParser());

    @Test
    @Deployment(resources = "test.bpmn")
    public void test() throws Exception {
        engineRule.startProcessInstanceByKey("test", null);
    }
}
