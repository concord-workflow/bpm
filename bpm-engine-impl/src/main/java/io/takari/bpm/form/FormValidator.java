package io.takari.bpm.form;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.form.FormSubmitResult.ValidationError;
import io.takari.bpm.model.form.FormField;

import java.util.List;
import java.util.Map;

public interface FormValidator {

    List<ValidationError> validate(Form form, Map<String, Object> data) throws ExecutionException;

    ValidationError validate(String formId, FormField f, Object v, Object allowed) throws ExecutionException;
}
