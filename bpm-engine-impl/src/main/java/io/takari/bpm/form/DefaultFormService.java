package io.takari.bpm.form;

import com.sun.org.apache.regexp.internal.RE;
import io.takari.bpm.api.Engine;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.form.FormSubmitResult.ValidationError;
import io.takari.bpm.model.form.FormDefinition;
import io.takari.bpm.model.form.FormField;
import io.takari.bpm.state.Variables;

import java.util.*;

public class DefaultFormService implements FormService {

    private final ResumeHandler resumeHandler;
    private final FormStorage formStorage;
    private final ExpressionManager expresssionManager;
    private final FormValidator validator;

    public DefaultFormService(ResumeHandler resumeHandler, FormStorage formStorage, ExpressionManager expresssionManager) {
        this(resumeHandler, formStorage, expresssionManager, new DefaultFormValidator());
    }

    public DefaultFormService(ResumeHandler resumeHandler,
                              FormStorage formStorage,
                              ExpressionManager expresssionManager,
                              FormValidator validator) {

        this.resumeHandler = resumeHandler;
        this.formStorage = formStorage;
        this.expresssionManager = expresssionManager;
        this.validator = validator;
    }

    @Override
    public void create(String processBusinessKey, UUID formInstanceId, String eventName,
                       FormDefinition formDefinition, Map<String, Object> env) throws ExecutionException {

        Form f = new Form(processBusinessKey, formInstanceId, eventName, formDefinition, env);
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
        Map<String, Object> values = new HashMap<>();
        for (FormField f : fd.getFields()) {
            String k = f.getName();
            Object v = defaults.get(k);

            String expr = f.getValueExpr();
            if (expr != null) {
                // create a new evaluation context for every expression - results should be independent
                Variables vars = new Variables(env);
                ExecutionContext ctx = new ExecutionContextImpl(em, vars);
                v = em.eval(ctx, expr, Object.class);
            }

            // validate the value we got, just in case if the default value or the expression value are incompatible
            // skip null values, they indicate empty (not filled) fields
            if (v != null) {
                ValidationError e = validator.validate(formName, f, v);
                if (e != null) {
                    throw new ExecutionException("Got an incompatible default value '%s' for field '%s': %s", v, k, e.getError());
                }
            }

            values.put(k, v);
        }

        // use the form's name to store its values
        env.put(formName, values);

        return new Form(form, env);
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
