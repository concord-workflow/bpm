package io.takari.bpm.model.form;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class FormDefinition implements Serializable {

    private final String name;
    private final List<FormField> fields;

    public FormDefinition(String name, FormField... fields) {
        this(name, Arrays.asList(fields));
    }

    public FormDefinition(String name, List<FormField> fields) {
        this.name = name;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public List<FormField> getFields() {
        return fields;
    }
}
