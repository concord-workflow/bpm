package io.takari.bpm.xml.bpmnjs.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(namespace = Constants.MODEL_NS, name = "serviceTask")
public class XmlServiceTask extends AbstractXmlElement {
    
    private String expression;
    private String delegateExpression;

    @XmlAttribute
    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @XmlAttribute
    public String getDelegateExpression() {
        return delegateExpression;
    }

    public void setDelegateExpression(String delegateExpression) {
        this.delegateExpression = delegateExpression;
    }
}
