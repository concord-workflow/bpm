package io.takari.bpm.form;

import io.takari.bpm.api.Engine;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.form.FormSubmitResult.ValidationError;
import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.form.FormDefinition;
import io.takari.bpm.model.form.FormField;
import io.takari.bpm.state.Variables;

import java.util.*;

public class DefaultFormService implements FormService {

    private final ResumeHandler resumeHandler;
    private final FormStorage formStorage;
    private final ExpressionManager expresssionManager;
    private final FormValidator validator;

    public DefaultFormService(ResumeHandler resumeHandler, FormStorage formStorage, ExpressionManager expressionManager) {
        this(resumeHandler, formStorage, expressionManager, new DefaultFormValidator());
    }

    public DefaultFormService(ResumeHandler resumeHandler,
                              FormStorage formStorage,
                              ExpressionManager expressionManager,
                              FormValidator validator) {

        this.resumeHandler = resumeHandler;
        this.formStorage = formStorage;
        this.expresssionManager = expressionManager;
        this.validator = validator;
    }

    @Override
    public void create(String processBusinessKey, UUID formInstanceId, String eventName,
                       FormDefinition formDefinition, Map<String, Object> env) throws ExecutionException {

        Form f = new Form(processBusinessKey, formInstanceId, eventName, formDefinition, env, Collections.emptyMap());
        f = prepare(expresssionManager, validator, f);
        formStorage.save(f);
    }

    @Override
    public Form get(UUID formInstanceId) throws ExecutionException {
        Form form = formStorage.get(formInstanceId);
        if (form == null) {
            throw new ExecutionException("Form not found: " + formInstanceId);
        }

        return formStorage.get(formInstanceId);
    }

    @Override
    public FormSubmitResult submit(UUID formInstanceId, Map<String, Object> data) throws ExecutionException {
        Form form = formStorage.get(formInstanceId);
        if (form == null) {
            throw new ExecutionException("Form not found: " + formInstanceId);
        }

        FormSubmitResult result = submit(resumeHandler, validator, form, data);
        if (result.isValid()) {
            formStorage.complete(formInstanceId);
        }

        return result;
    }

    public static Form prepare(ExpressionManager em, FormValidator validator, Form form) throws ExecutionException {
        // make a copy of the form's environment
        Map<String, Object> env = form.getEnv();
        env = new HashMap<>(env != null ? env : Collections.emptyMap());

        FormDefinition fd = form.getFormDefinition();
        String formName = fd.getName();

        Map<String, Object> defaults = (Map<String, Object>) env.get(formName);
        if (defaults == null) {
            defaults = Collections.emptyMap();
        }

        // fill the form's values either using provided defaults or by eval'ing field expressions
        Map<String, Object> values = new HashMap<>(defaults);

        // calculate and store allowed values for the form's fields
        Map<String, Object> allowedValues = form.getAllowedValues();
        allowedValues = new HashMap<>(allowedValues != null ? allowedValues : Collections.emptyMap());

        for (FormField f : fd.getFields()) {
            String k = f.getName();
            Object v = defaults.get(k);

            Object defaultValue = f.getDefaultValue();
            if (defaultValue != null) {
                // create a new evaluation context for every expression - results should be independent
                Variables vars = new Variables(env);
                ExecutionContext ctx = new ExecutionContextImpl(em, vars);
                v = em.interpolate(ctx, defaultValue);
            }

            Object allowedValue = f.getAllowedValue();
            if (allowedValue != null) {
                // same deal: use a new context every time
                Variables vars = new Variables(env);
                ExecutionContext ctx = new ExecutionContextImpl(em, vars);
                allowedValue = em.interpolate(ctx, allowedValue);
                if (allowedValue != null) {
                    allowedValues.put(f.getName(), allowedValue);
                }
            }

            if (v == null) {
                continue;
            }

            // validate the value we've got, just in case if the default value or the expression's value are
            // incompatible. Skip null values, they indicate empty (not yet filled) fields.
            ValidationError e = validator.validate(formName, f, v, allowedValue);
            if (e != null) {
                throw new ExecutionException("Got an incompatible default value '%s'. %s", v, e.getError());
            }

            values.put(k, v);
        }

        // use the form's name to store its values
        env.put(formName, values);

        return new Form(form, env, allowedValues);
    }

    public static FormSubmitResult submit(ResumeHandler resumeHandler,
                                          FormValidator validator,
                                          Form form,
                                          Map<String, Object> data) throws ExecutionException {

        String processBusinessKey = form.getProcessBusinessKey();

        List<ValidationError> errors = validator.validate(form, data);
        if (errors != null && !errors.isEmpty()) {
            return new FormSubmitResult(processBusinessKey, errors);
        }

        FormDefinition fd = form.getFormDefinition();

        // the new form's values will be available under the form's name key
        Map<String, Object> args = new HashMap<>();
        args.put(fd.getName(), new HashMap<>(data));
        resumeHandler.resume(form, args);

        return FormSubmitResult.ok(processBusinessKey);
    }

    public interface ResumeHandler {

        void resume(Form form, Map<String, Object> args) throws ExecutionException;
    }

    public static class NoopResumeHandler implements ResumeHandler {

        @Override
        @CoverageIgnore
        public void resume(Form form, Map<String, Object> args) throws ExecutionException {
        }
    }

    public static class DirectResumeHandler implements ResumeHandler {

        private final Engine engine;

        public DirectResumeHandler(Engine engine) {
            this.engine = engine;
        }

        @Override
        public void resume(Form form, Map<String, Object> args) throws ExecutionException {
            engine.resume(form.getProcessBusinessKey(), form.getEventName(), args);
        }
    }
}
