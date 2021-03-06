package io.takari.bpm.form;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.form.FormDefinition;
import io.takari.bpm.model.form.FormField;

import java.util.Map;
import java.util.UUID;

/**
 * Service to retrieve and submit form data.
 */
public interface FormService {

    /**
     * Creates a new form instance. The default values will be calculated using the provided enviroment.
     * @param processBusinessKey
     * @param formInstanceId ID of a new form instance.
     * @param eventName the name of an event, which can be used to resume the process.
     * @param formDefinition
     * @param options form call options.
     * @param env process environment.
     * @throws ExecutionException
     */
    void create(String processBusinessKey, UUID formInstanceId, String eventName,
                FormDefinition formDefinition, Map<String, Object> options,
                Map<String, Object> env) throws ExecutionException;

    /**
     * Retrieve a form.
     * @param formInstanceId ID of a form instance.
     * @return initial form values.
     * @throws ExecutionException
     */
    Form get(UUID formInstanceId) throws ExecutionException;

    /**
     * Submit form data, potentially triggering the continuation of a process.
     * @param formInstanceId ID of a form instance.
     * @param data form data.
     * @return submission results, including validation errors.
     * @throws ExecutionException
     */
    FormSubmitResult submit(UUID formInstanceId, Map<String, Object> data) throws ExecutionException;

    /**
     * Converts the supplied data into a form field.
     * @param m input data. Typically a map with a single entry: {@code key -> value},
     *          where {@code key} is the name of the field and {@value} are the field's options.
     * @return a form field.
     */
    default FormField toFormField(Map<String, Object> m) {
        throw new IllegalStateException("Not implemented");
    }
}
