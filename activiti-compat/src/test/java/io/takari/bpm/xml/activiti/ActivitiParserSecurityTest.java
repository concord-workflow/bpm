package io.takari.bpm.xml.activiti;

import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.TerminateEvent;
import io.takari.bpm.xml.ParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ActivitiParserSecurityTest {

    @TempDir
    Path tempDir;

    private static ProcessDefinition parse(String xml) throws ParserException {
        return new ActivitiParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String process(String body) {
        return "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">" +
                "<process id=\"test\">" + body + "</process></definitions>";
    }

    @Test
    public void testExternalEntitiesAreRejected() throws Exception {
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "s3cr3t");

        String xml = "<?xml version=\"1.0\"?>" +
                "<!DOCTYPE definitions [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>" +
                process("<startEvent id=\"start\"/>" +
                        "<sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"end\">" +
                        "<conditionExpression>&xxe;</conditionExpression></sequenceFlow>" +
                        "<endEvent id=\"end\"/>");

        ParserException e = assertThrows(ParserException.class, () -> parse(xml));
        assertFalse(String.valueOf(e.getCause()).contains("s3cr3t"));
    }

    @Test
    public void testUnsupportedElementsAreRejected() {
        String xml = process("<startEvent id=\"start\"/>" +
                "<sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"t1\"/>" +
                "<userTask id=\"t1\"/>" +
                "<sequenceFlow id=\"f2\" sourceRef=\"t1\" targetRef=\"end\"/>" +
                "<endEvent id=\"end\"/>");

        ParserException e = assertThrows(ParserException.class, () -> parse(xml));
        assertTrue(e.getCause().getMessage().contains("Unsupported element: userTask"), e.getCause().getMessage());
    }

    @Test
    public void testEventSubprocessIsRejected() {
        String xml = process("<startEvent id=\"start\"/>" +
                "<sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"end\"/>" +
                "<endEvent id=\"end\"/>" +
                "<subProcess id=\"sub\" triggeredByEvent=\"true\"><startEvent id=\"sstart\"/></subProcess>");

        assertThrows(ParserException.class, () -> parse(xml));
    }

    @Test
    public void testTerminateEndEvent() throws Exception {
        String xml = process("<startEvent id=\"start\"/>" +
                "<sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"end\"/>" +
                "<endEvent id=\"end\"><terminateEventDefinition/></endEvent>");

        ProcessDefinition pd = parse(xml);
        assertInstanceOf(TerminateEvent.class, pd.getChild("end"));
        assertInstanceOf(SequenceFlow.class, pd.getChild("f1"));
    }
}
