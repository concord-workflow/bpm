package io.takari.bpm.xml.bpmnjs;

import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.EventBasedGateway;
import io.takari.bpm.model.ExclusiveGateway;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.InclusiveGateway;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.xml.Parser;
import io.takari.bpm.xml.ParserException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import io.takari.bpm.xml.bpmnjs.model.AbstractXmlElement;
import io.takari.bpm.xml.bpmnjs.model.XmlDefinitions;
import io.takari.bpm.xml.bpmnjs.model.XmlEndEvent;
import io.takari.bpm.xml.bpmnjs.model.XmlEventBasedGateway;
import io.takari.bpm.xml.bpmnjs.model.XmlExclusiveGateway;
import io.takari.bpm.xml.bpmnjs.model.XmlInclusiveGateway;
import io.takari.bpm.xml.bpmnjs.model.XmlIntermediateCatchEvent;
import io.takari.bpm.xml.bpmnjs.model.XmlIntermediateThrowEvent;
import io.takari.bpm.xml.bpmnjs.model.XmlProcess;
import io.takari.bpm.xml.bpmnjs.model.XmlSequenceFlow;
import io.takari.bpm.xml.bpmnjs.model.XmlServiceTask;
import io.takari.bpm.xml.bpmnjs.model.XmlStartEvent;

public class BpmnJsParser implements Parser {
    
    private final JAXBContext ctx;

    public BpmnJsParser() {
        try {
            ctx = JAXBContext.newInstance("io.takari.bpm.xml.bpmnjs.model");
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProcessDefinition parse(InputStream in) throws ParserException {
        try {
            Unmarshaller m = ctx.createUnmarshaller();
            Object src = m.unmarshal(in);
            return convert((XmlDefinitions) src);
        } catch (JAXBException e) {
            throw new ParserException("JAXB error", e);
        }
    }
    
    private static ProcessDefinition convert(XmlDefinitions src) {
        XmlProcess p = src.getProcess();
        List<AbstractElement> children = new ArrayList<>();
        
        for (AbstractXmlElement e : p.getElements()) {
            if (e instanceof XmlStartEvent) {
                children.add(new StartEvent(e.getId()));
            } else if (e instanceof XmlEndEvent) {
                XmlEndEvent end = (XmlEndEvent) e;
                if (end.getErrorEventDefinition() != null) {
                    children.add(new EndEvent(e.getId(), end.getErrorEventDefinition().getErrorRef()));
                } else {
                    children.add(new EndEvent(e.getId()));
                }
            } else if (e instanceof XmlSequenceFlow) {
                XmlSequenceFlow f = (XmlSequenceFlow) e;
                children.add(new SequenceFlow(e.getId(), f.getSourceRef(), f.getTargetRef(), f.getExpression()));
            } else if (e instanceof XmlServiceTask) {
                XmlServiceTask t = (XmlServiceTask) e;
                
                String exp = null;
                ExpressionType type = ExpressionType.NONE;
                if (t.getExpression() != null) {
                    type = ExpressionType.SIMPLE;
                    exp = t.getExpression();
                } else if (t.getDelegateExpression() != null) {
                    type = ExpressionType.DELEGATE;
                    exp = t.getDelegateExpression();
                }
                
                children.add(new ServiceTask(e.getId(), type, exp));
            } else if (e instanceof XmlEventBasedGateway) {
                children.add(new EventBasedGateway(e.getId()));
            } else if (e instanceof XmlExclusiveGateway) {
                // TODO default flow
                children.add(new ExclusiveGateway(e.getId()));
            } else if (e instanceof XmlInclusiveGateway) {
                children.add(new InclusiveGateway(e.getId()));
            } else if (e instanceof XmlIntermediateCatchEvent) {
                // TODO message refs & timers
                children.add(new IntermediateCatchEvent(e.getId()));
            } else if (e instanceof XmlIntermediateThrowEvent) {
                // TODO attachments, error refs and timeouts
                children.add(new BoundaryEvent(e.getId(), null, null));
            }
        }
        
        ProcessDefinition def = new ProcessDefinition(p.getId(), children);
        return def;
    }
}
