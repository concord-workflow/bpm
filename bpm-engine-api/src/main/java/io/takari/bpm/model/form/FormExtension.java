package io.takari.bpm.model.form;

import io.takari.bpm.model.UserTask.Extension;

public class FormExtension implements Extension {

    private final String formId;

    public FormExtension(String formId) {
        this.formId = formId;
    }

    public String getFormId() {
        return formId;
    }

    @Override
    public String toString() {
        return "FormExtension{" +
                "formId='" + formId + '\'' +
                '}';
    }
}
