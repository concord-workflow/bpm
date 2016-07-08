package io.takari.bpm.xml.bpmnjs;

import io.takari.bpm.xml.bpmnjs.BpmnJsParser;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.xml.Parser;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BpmnJsParserTest {
    
    @Test
    public void testSimple() throws Exception {
        InputStream in = ClassLoader.getSystemResourceAsStream("camunda.bpmn");
        Parser p = new BpmnJsParser();

        ProcessDefinition pd = p.parse(in);
        assertNotNull(pd);
        assertEquals("Process_1", pd.getId());
    }
    
    @Test
    public void testComplex() throws Exception {
        InputStream in = ClassLoader.getSystemResourceAsStream("complex.bpmn");
        Parser p = new BpmnJsParser();

        ProcessDefinition pd = p.parse(in);
        assertNotNull(pd);
        assertNotNull(pd.getChildren());
        assertEquals(21, pd.getChildren().size());
    }
}
