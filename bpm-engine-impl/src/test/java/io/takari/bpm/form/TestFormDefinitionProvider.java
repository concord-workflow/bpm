package io.takari.bpm.form;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.form.FormDefinition;

import java.util.HashMap;
import java.util.Map;

public class TestFormDefinitionProvider implements FormDefinitionProvider {

    private final Map<String, FormDefinition> forms = new HashMap<>();

    @Override
    public FormDefinition getById(String id) throws ExecutionException {
        return forms.get(id);
    }

    public void deploy(FormDefinition fd) {
        forms.put(fd.getName(), fd);
    }
}
