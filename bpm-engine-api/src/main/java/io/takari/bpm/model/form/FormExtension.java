package io.takari.bpm.model.form;

import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.UserTask.Extension;

import java.util.Map;

public class FormExtension implements Extension {

    private final String formId;
    private final Map<String, Object> options;

    public FormExtension(String formId) {
        this(formId, null);
    }

    public FormExtension(String formId, Map<String, Object> options) {
        this.formId = formId;
        this.options = options;
    }

    public String getFormId() {
        return formId;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "FormExtension{" +
                "formId='" + formId + '\'' +
                ", options=" + options +
                '}';
    }
}
