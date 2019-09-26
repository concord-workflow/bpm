package io.takari.bpm.form;

import io.takari.bpm.api.Engine;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.context.DefaultExecutionContextFactory;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.el.DefaultExpressionManager;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.form.DefaultFormService.ResumeHandler;
import io.takari.bpm.model.form.DefaultFormFields.BooleanField;
import io.takari.bpm.model.form.DefaultFormFields.DecimalField;
import io.takari.bpm.model.form.DefaultFormFields.IntegerField;
import io.takari.bpm.model.form.DefaultFormFields.StringField;
import io.takari.bpm.model.form.FormDefinition;
import io.takari.bpm.model.form.FormField;
import io.takari.bpm.model.form.FormField.Cardinality;
import io.takari.bpm.task.ServiceTaskRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class FormServiceTest {

    private FormValidatorLocale formLocale;
    private FormStorage formStorage;
    private FormService formService;

    @Before
    public void setUp() {
        Engine engine = mock(Engine.class);

        ServiceTaskRegistry taskRegistry = mock(ServiceTaskRegistry.class);
        ExpressionManager expressionManager = new DefaultExpressionManager(taskRegistry);

        formStorage = new InMemFormStorage();

        ExecutionContextFactory contextFactory = new DefaultExecutionContextFactory(expressionManager);
        ResumeHandler resumeHandler = (form, args) -> engine.resume(form.getProcessBusinessKey(), form.getEventName(), args);

        formLocale = spy(new DefaultFormValidatorLocale());
        FormValidator validator = new DefaultFormValidator(formLocale);

        formService = new DefaultFormService(contextFactory, resumeHandler, formStorage, validator);
    }

    private static FormField stringValue(String fieldName) {
        return new FormField.Builder(fieldName, StringField.TYPE)
                .build();
    }

    private static FormField stringOptional(String fieldName) {
        return new FormField.Builder(fieldName, StringField.TYPE)
                .cardinality(Cardinality.ONE_OR_NONE)
                .build();
    }

    private static FormField stringOneOrMore(String fieldName) {
        return new FormField.Builder(fieldName, StringField.TYPE)
                .cardinality(Cardinality.AT_LEAST_ONE)
                .build();
    }

    private static FormField stringOneOrMore(String fieldName, Object defaultValue) {
        return new FormField.Builder(fieldName, StringField.TYPE)
                .cardinality(Cardinality.AT_LEAST_ONE)
                .defaultValue(defaultValue)
                .build();
    }

    private static FormField stringPattern(String fieldName, String pattern) {
        return new FormField.Builder(fieldName, StringField.TYPE)
                .cardinality(Cardinality.ONE_OR_NONE)
                .option(StringField.PATTERN, pattern)
                .build();
    }

    private static FormField intRange(String fieldName, long min, long max) {
        return new FormField.Builder(fieldName, IntegerField.TYPE)
                .cardinality(Cardinality.ONE_OR_NONE)
                .option(IntegerField.MIN, min)
                .option(IntegerField.MAX, max)
                .build();
    }

    private static FormField intList(String fieldName, long min, long max) {
        return new FormField.Builder(fieldName, IntegerField.TYPE)
                .cardinality(Cardinality.AT_LEAST_ONE)
                .option(IntegerField.MIN, min)
                .option(IntegerField.MAX, max)
                .build();
    }

    private static FormField decimalRange(String fieldName, double min, double max) {
        return new FormField.Builder(fieldName, DecimalField.TYPE)
                .cardinality(Cardinality.ONE_OR_NONE)
                .option(DecimalField.MIN, min)
                .option(DecimalField.MAX, max)
                .build();
    }

    private static FormField booleanValue(String fieldName) {
        return new FormField.Builder(fieldName, BooleanField.TYPE)
                .build();
    }

    @Test(expected = ExecutionException.class)
    public void testInvalidType() throws Exception {
        FormSubmitResult r = submit(new FormField[]{new FormField.Builder("a", "zzz").build()},
                "a", "test");
        assertFalse(r.isValid());
    }

    @Test
    public void testValidSingleString() throws Exception {
        FormSubmitResult r = submit(new FormField[]{stringValue("a")},
                "a", "test");
        assertTrue(r.isValid());
    }

    @Test
    public void testInvalidMissingString() throws Exception {
        FormField f = stringValue("a");
        FormSubmitResult r = submit(new FormField[]{f});
        assertFalse(r.isValid());

        verify(formLocale, times(1)).invalidCardinality(anyString(), any(FormField.class), anyObject());
    }

    @Test
    public void testValidMissingString() throws Exception {
        FormSubmitResult r = submit(new FormField[]{stringOptional("a")});
        assertTrue(r.isValid());
    }

    @Test
    public void testInvalidEmptyArrayOfStrings() throws Exception {
        FormField f = stringOneOrMore("a");
        FormSubmitResult r = submit(new FormField[]{f},
                "a", new String[0]);
        assertFalse(r.isValid());

        verify(formLocale, times(1)).invalidCardinality(anyString(), any(FormField.class), anyObject());
    }

    @Test
    public void testValidArrayOfStrings() throws Exception {
        FormSubmitResult r = submit(new FormField[]{stringOneOrMore("a")},
                "a", new String[]{"a", "b", "c"});
        assertTrue(r.isValid());
    }

    @Test
    public void testValidDefaultArrayOfStrings() throws Exception {
        FormSubmitResult r = submit(new FormField[]{stringOneOrMore("a", new Object[]{"a", "b", "c"})});
        assertTrue(r.isValid());
    }

    @Test
    public void testInvalidArrayOfMixedTypes() throws Exception {
        FormField f = stringOneOrMore("a");
        FormSubmitResult r = submit(new FormField[]{f},
                "a", new Object[]{"a", 1, false});
        assertFalse(r.isValid());

        verify(formLocale, times(1)).expectedString(anyString(), any(FormField.class), eq(1), anyObject());
    }

    @Test
    public void testInvalidCollectionOfMixedTypes() throws Exception {
        FormField f = stringOneOrMore("a");
        FormSubmitResult r = submit(new FormField[]{f},
                "a", Arrays.asList("a", "b", 123));
        assertFalse(r.isValid());

        verify(formLocale, times(1)).expectedString(anyString(), any(FormField.class), eq(2), anyObject());
    }

    @Test
    public void testInvalidStringPattern() throws Exception {
        FormField f = stringPattern("a", "[0-9]+");
        FormSubmitResult r = submit(new FormField[]{f},
                "a", "abc");
        assertFalse(r.isValid());

        verify(formLocale, times(1)).doesntMatchPattern(anyString(), any(FormField.class), any(Integer.class), anyString(), anyObject());
    }

    @Test
    public void testExpectedString() throws Exception {
        FormField f = stringPattern("a", "[0-9]+");
        FormSubmitResult r = submit(new FormField[]{f},
                "a", 123);
        assertFalse(r.isValid());

        verify(formLocale, times(1)).expectedString(anyString(), any(FormField.class), any(Integer.class), anyObject());
    }

    @Test
    public void testDefaultValueString() throws Exception {
        Map<String, Object> env = Collections.singletonMap("b", "abc");

        FormField f = new FormField.Builder("a", StringField.TYPE)
                .defaultValue("${b}")
                .build();

        FormSubmitResult r = submit(new FormField[]{f}, env);
        assertTrue(r.isValid());
    }

    @Test(expected = ExecutionException.class)
    public void testInvalidDefaultValueString() throws Exception {
        Map<String, Object> env = Collections.singletonMap("b", 123);

        FormField f = new FormField.Builder("a", StringField.TYPE)
                .defaultValue("${b}")
                .build();

        FormSubmitResult r = submit(new FormField[]{f}, env);
        assertFalse(r.isValid());
    }

    @Test
    public void testValidIntegerRange() throws Exception {
        FormSubmitResult r = submit(new FormField[]{intRange("a", 10, 100)},
                "a", 12);
        assertTrue(r.isValid());
    }

    @Test
    public void testValidIntegerArray() throws Exception {
        FormSubmitResult r = submit(new FormField[]{intList("a", 0, 4)},
                "a", new int[]{0, 1, 2, 3, 4});
        assertTrue(r.isValid());
    }

    @Test
    public void testInvalidIntegerArray() throws Exception {
        FormSubmitResult r = submit(new FormField[]{intList("a", 0, 5)},
                "a", new int[]{2, 3, 4, 5, 6});
        assertFalse(r.isValid());
    }

    @Test
    public void testValidIntegerList() throws Exception {
        FormSubmitResult r = submit(new FormField[]{intList("a", 0, 5)},
                "a", Arrays.asList(0, 1, 2, 3, 4, 5));
        assertTrue(r.isValid());
    }

    @Test
    public void testValidDecimalRange() throws Exception {
        FormSubmitResult r = submit(new FormField[]{decimalRange("a", 0.5, 0.52)},
                "a", 0.51);
        assertTrue(r.isValid());
    }

    @Test
    public void testInvalidDecimalRange() throws Exception {
        FormField f = decimalRange("a", 0.5, 0.52);
        FormSubmitResult r = submit(new FormField[]{f},
                "a", 0.525);
        assertFalse(r.isValid());

        verify(formLocale, times(1)).decimalRangeError(anyString(), any(FormField.class), any(Integer.class), any(Double.class), any(Double.class), anyObject());
    }

    @Test
    public void testValidSingleBoolean() throws Exception {
        FormSubmitResult r = submit(new FormField[]{booleanValue("a")},
                "a", true);
        assertTrue(r.isValid());
    }

    private FormSubmitResult submit(FormField[] fields, Object... values) throws ExecutionException {
        return submit(fields, null, values);
    }

    private FormSubmitResult submit(FormField[] fields, Map<String, Object> env, Object... values) throws ExecutionException {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Values must be in pairs");
        }

        FormDefinition fd = new FormDefinition("test#" + System.currentTimeMillis(), fields);

        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            m.put((String) values[i], values[i + 1]);
        }

        return submit(fd, env, m);
    }

    private FormSubmitResult submit(FormDefinition fd, Map<String, Object> env, Map<String, Object> values) throws ExecutionException {
        String key = UUID.randomUUID().toString();
        String eventName = UUID.randomUUID().toString();
        UUID formInstanceId = UUID.randomUUID();
        formService.create(key, formInstanceId, eventName, fd, null, env);

        Form f = formService.get(formInstanceId);
        String dataKey = f.getFormDefinition().getName();
        Map<String, Object> m = (Map<String, Object>) f.getEnv().get(dataKey);
        m.putAll(values);

        return formService.submit(formInstanceId, m);
    }
}
