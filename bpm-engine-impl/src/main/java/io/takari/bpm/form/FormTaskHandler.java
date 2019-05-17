package io.takari.bpm.form;

import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.CreateEventAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.Variables;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.UserTask;
import io.takari.bpm.model.UserTask.Extension;
import io.takari.bpm.model.form.FormDefinition;
import io.takari.bpm.model.form.FormExtension;
import io.takari.bpm.model.form.FormField;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.StateHelper;
import io.takari.bpm.task.UserTaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FormTaskHandler implements UserTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(FormTaskHandler.class);

    private final ExecutionContextFactory contextFactory;
    private final FormDefinitionProvider formDefinitionProvider;
    private final FormService formService;

    public FormTaskHandler(ExecutionContextFactory contextFactory,
                           FormDefinitionProvider formDefinitionProvider,
                           FormService formService) {

        this.contextFactory = contextFactory;
        this.formDefinitionProvider = formDefinitionProvider;
        this.formService = formService;
    }

    @Override
    public ProcessInstance handle(ProcessInstance state, String definitionId, String elementId) throws ExecutionException {
        ProcessDefinition pd = state.getDefinition(definitionId);
        UserTask task = (UserTask) ProcessDefinitionUtils.findElement(pd, elementId);

        FormExtension x = findFormExtension(task.getExtensions());
        if (x == null) {
            return state;
        }

        log.debug("handle ['{}', '{}', '{}'] -> found form extension: {}",
                state.getBusinessKey(), definitionId, elementId, x);

        Map<String, Object> options = getOptions(x, state.getVariables());

        String formId = resolveFormId(x, state.getVariables());
        FormDefinition fd = getOrCreateFormDefinition(formId, options);
        if (fd == null) {
            throw new ExecutionException("Form definition not found: " + formId);
        }

        String pk = state.getBusinessKey();
        UUID fId = UUID.randomUUID();
        String eventName = UUID.randomUUID().toString();
        Map<String, Object> env = state.getVariables().asMap();

        formService.create(pk, fId, eventName, fd, options, env);

        return StateHelper.push(state, new CreateEventAction(definitionId, elementId, eventName, null, null, null, null, false));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOptions(FormExtension x, Variables vars) {
        ExecutionContext ctx = contextFactory.create(vars);
        return (Map<String, Object>) ctx.interpolate(x.getOptions());
    }

    private String resolveFormId(FormExtension x, Variables vars) {
        if (x.getFormIdExpression() == null) {
            return x.getFormId();
        }

        ExecutionContext ctx = contextFactory.create(vars);
        return (String) ctx.interpolate(x.getFormIdExpression());
    }

    private FormDefinition getOrCreateFormDefinition(String formId, Map<String, Object> options) throws ExecutionException {
        Object v = options != null ? options.get("fields") : null;
        if (v == null) {
            return formDefinitionProvider.getById(formId);
        }

        if (!(v instanceof List)) {
            throw new IllegalArgumentException("Expected a list of fields in form '" + formId + "', got: " + v.getClass());
        }

        return new FormDefinition(formId, coerceToFormFields((List<?>) v));
    }

    @SuppressWarnings("unchecked")
    private List<FormField> coerceToFormFields(List<?> input) {
        List<FormField> result = new ArrayList<>(input.size());

        for (Object v : input) {
            if (v instanceof FormField) {
                result.add((FormField) v);
            } else if (v instanceof Map) {
                FormField f = formService.toFormField((Map<String, Object>) v);
                result.add(f);
            } else {
                throw new IllegalArgumentException("Expected either a FormField instance or a map of values, got: " + v);
            }
        }

        return result;
    }

    private static FormExtension findFormExtension(Collection<Extension> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return null;
        }

        for (Extension e : extensions) {
            if (e instanceof FormExtension) {
                return (FormExtension) e;
            }
        }

        return null;
    }
}
