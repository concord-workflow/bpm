package io.takari.bpm.model.form;

import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.UserTask.Extension;

import java.util.Map;

public class FormExtension implements Extension {

    private static final long serialVersionUID = -6059980075281292420L;

    private final String formId;
    private final String formIdExpression;
    private final Map<String, Object> options;

    public FormExtension(String formId) {
        this(formId, null, null);
    }

    public FormExtension(String formId, String formIdExpression) {
        this(formId, formIdExpression, null);
    }

    public FormExtension(String formId, Map<String, Object> options) {
        this(formId, null, options);
    }

    public FormExtension(String formId, String formIdExpression, Map<String, Object> options) {
        this.formId = formId;
        this.formIdExpression = formIdExpression;
        this.options = options;
    }

    public String getFormId() {
        return formId;
    }

    public String getFormIdExpression() {
        return formIdExpression;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "FormExtension{" +
                "formId='" + formId + '\'' +
                ", formIdExpression='" + formIdExpression + '\'' +
                ", options=" + options +
                '}';
    }
}
