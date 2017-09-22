package io.takari.bpm.form;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.form.FormSubmitResult.ValidationError;
import io.takari.bpm.model.*;
import io.takari.bpm.model.form.DefaultFormFields.StringField;
import io.takari.bpm.model.form.FormDefinition;
import io.takari.bpm.model.form.FormExtension;
import io.takari.bpm.model.form.FormField;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class FormTest extends AbstractFormTest {


    /**
     * start --> t1 --> t2 --> end
     */
    @Test
    public void testSimple() throws Exception {
        String formId = "testForm";
        String formField = "testValue";
        String testValue = "test#" + System.currentTimeMillis();

        formDefinitionProvider.deploy(new FormDefinition(formId,
                new FormField.Builder(formField, StringField.TYPE)
                        .option(StringField.PATTERN, testValue)
                        .build()));

        // ---

        JavaDelegate t2 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                Object v = ctx.eval("${" + formId + "." + formField + "}", Object.class);
                assertEquals(testValue, v);
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new UserTask("t1", new FormExtension(formId)),
                new SequenceFlow("f2", "t1", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f3", "t2", "end"),
                new EndEvent("end")
        ));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        assertActivations(key, processId,
                "start",
                "f1",
                "t1");

        // ---

        UUID formInstanceId = formRegistry.getForms().keySet().iterator().next();
        assertNotNull(formInstanceId);

        FormSubmitResult r = formService.submit(formInstanceId, Collections.singletonMap(formField, "abc"));
        assertFalse(r.isValid());
        assertEquals(1, r.getErrors().size());

        ValidationError e = r.getErrors().get(0);
        assertEquals(formField, e.getFieldName());

        verifyZeroInteractions(t2);

        // --

        r = formService.submit(formInstanceId, Collections.singletonMap("testValue", testValue));
        assertTrue(r.isValid());

        // ---

        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> t1 --> end
     */
    @Test
    public void testDefaultValue() throws Exception {
        String formId = "testForm";
        String formField = "testValue";

        formDefinitionProvider.deploy(new FormDefinition(formId,
                new FormField.Builder(formField, StringField.TYPE)
                        .allowedValue(Arrays.asList("a", "b", "c"))
                        .build()));

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new UserTask("t1", new FormExtension(formId)),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        ));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        assertActivations(key, processId,
                "start",
                "f1",
                "t1");

        // ---

        UUID formInstanceId = formRegistry.getForms().keySet().iterator().next();
        assertNotNull(formInstanceId);

        FormSubmitResult r = formService.submit(formInstanceId, Collections.singletonMap(formField, "b"));
        assertTrue(r.isValid());
    }

    /**
     * start --> t1 --> end
     */
    @Test
    public void testOptionsInterpolation() throws Exception {
        String inputVar = "var_" + System.currentTimeMillis();
        String inputVal = "val_" + System.currentTimeMillis();

        String formId = "testForm";

        formDefinitionProvider.deploy(new FormDefinition(formId,
                new FormField.Builder("testField", StringField.TYPE)
                        .cardinality(FormField.Cardinality.ANY)
                        .build()));

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t1"),
                new UserTask("t1", new FormExtension(formId, Collections.singletonMap("opt", "${" + inputVar + "}"))),
                new SequenceFlow("f2", "t1", "end"),
                new EndEvent("end")
        ));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, Collections.singletonMap(inputVar, inputVal));

        assertActivations(key, processId,
                "start",
                "f1",
                "t1");

        // ---

        UUID formInstanceId = formRegistry.getForms().keySet().iterator().next();
        assertNotNull(formInstanceId);

        FormSubmitResult r = formService.submit(formInstanceId, Collections.emptyMap());
        assertTrue(r.isValid());

        // ---

        ArgumentCaptor<Map> opts = ArgumentCaptor.forClass(Map.class);
        verify(formService, times(1)).create(eq(key), any(UUID.class), anyString(), any(FormDefinition.class),
                opts.capture(), any(Map.class));

        Map m = opts.getValue();
        assertEquals(inputVal, m.get("opt"));
    }
}
