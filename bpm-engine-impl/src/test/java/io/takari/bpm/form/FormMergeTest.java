package io.takari.bpm.form;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import io.takari.bpm.model.form.DefaultFormFields.StringField;
import io.takari.bpm.model.form.FormDefinition;
import io.takari.bpm.model.form.FormExtension;
import io.takari.bpm.model.form.FormField;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FormMergeTest extends AbstractFormTest {

    public FormMergeTest() {
        super(engine -> (form, args) -> engine.resume(form.getProcessBusinessKey(), form.getEventName(), args, true));
    }

    /**
     * start --> t1 --> t2 --> end
     */
    @Test
    public void testMerge() throws Exception {
        String formId = "testForm";

        String formVar = "fVar_" + System.currentTimeMillis();
        String formVal = "fVal_" + System.currentTimeMillis();

        String formVarFull = formId + "." + formVar;

        String externalVar = "var_" + System.currentTimeMillis();
        String externalVal = "val_" + System.currentTimeMillis();

        String externalVarFull = formId + ".nested." + externalVar;

        formDefinitionProvider.deploy(new FormDefinition(formId,
                new FormField.Builder(formVar, StringField.TYPE)
                        .cardinality(FormField.Cardinality.ONE_AND_ONLY_ONE)
                        .build()));

        // ---

        JavaDelegate t0 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                assertEquals(externalVal, ctx.eval("${" + externalVarFull + "}", Object.class));
            }
        });
        getServiceTaskRegistry().register("t0", t0);

        TestBean t2 = spy(new TestBean() {
            @Override
            public void test(Object a, Object b) {
                assertEquals(formVal, a);
                assertEquals(externalVal, b);
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId,
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "t0"),
                new ServiceTask("t0", ExpressionType.DELEGATE, "${t0}"),
                new SequenceFlow("f2", "t0", "t1"),
                new UserTask("t1", new FormExtension(formId)),
                new SequenceFlow("f3", "t1", "t2"),
                new ServiceTask("t2", ExpressionType.SIMPLE, "${t2.test(" + formVarFull + ", " + externalVarFull + ")}"),
                new SequenceFlow("f4", "t2", "end"),
                new EndEvent("end")
        ));

        // ---

        Map<String, Object> args = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put(externalVar, externalVal);
        args.put(formId, Collections.singletonMap("nested", nested));

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, args);

        // ---

        UUID formInstanceId = formRegistry.getForms().keySet().iterator().next();
        assertNotNull(formInstanceId);

        FormSubmitResult r = formService.submit(formInstanceId, Collections.singletonMap(formVar, formVal));
        assertTrue(r.isValid());

        // ---

        verify(t2, times(1)).test(anyObject(), anyObject());
    }

    public interface TestBean {

        void test(Object a, Object b);
    }
}
