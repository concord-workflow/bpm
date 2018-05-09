package io.takari.bpm.form;

import io.takari.bpm.AbstractEngineTest;
import io.takari.bpm.api.Engine;
import io.takari.bpm.context.DefaultExecutionContextFactory;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.el.DefaultExpressionManager;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.form.DefaultFormService.DirectResumeHandler;
import io.takari.bpm.form.DefaultFormService.ResumeHandler;
import io.takari.bpm.task.ServiceTaskRegistry;
import org.junit.Before;

import java.util.function.Function;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class AbstractFormTest extends AbstractEngineTest {

    protected InMemFormStorage formRegistry;
    protected FormService formService;
    private FormValidatorLocale formLocale;
    protected TestFormDefinitionProvider formDefinitionProvider;

    private final Function<Engine, ResumeHandler> resumeHandlerFn;

    public AbstractFormTest() {
        this(engine -> new DirectResumeHandler(engine));
    }

    public AbstractFormTest(Function<Engine, ResumeHandler> resumeHandlerFn) {
        this.resumeHandlerFn = resumeHandlerFn;
    }

    @Before
    public void setUp() {
        ServiceTaskRegistry taskRegistry = mock(ServiceTaskRegistry.class);
        ExpressionManager expressionManager = new DefaultExpressionManager(taskRegistry);

        formRegistry = new InMemFormStorage();

        formLocale = spy(new DefaultFormValidatorLocale());
        FormValidator validator = new DefaultFormValidator(formLocale);

        ResumeHandler resumeHandler = resumeHandlerFn.apply(getEngine());
        ExecutionContextFactory contextFactory = new DefaultExecutionContextFactory(expressionManager);
        formService = spy(new DefaultFormService(contextFactory, resumeHandler, formRegistry, validator));

        formDefinitionProvider = new TestFormDefinitionProvider();
        getUserTaskHandler().set(new FormTaskHandler(contextFactory, formDefinitionProvider, formService));
    }
}
