package io.takari.bpm.form;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.form.FormDefinition;

public interface FormDefinitionProvider {

    FormDefinition getById(String id) throws ExecutionException;
}
