package io.takari.bpm.el;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.task.ServiceTaskRegistry;
import io.takari.bpm.task.ServiceTaskResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.el.*;

public class DefaultExpressionManager implements ExpressionManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultExpressionManager.class);

    private final ExpressionFactory expressionFactory = new ExpressionFactoryImpl();
    private final ELResolver[] resolvers;

    public DefaultExpressionManager(ServiceTaskRegistry serviceTaskRegistry) {
        this.resolvers = new ELResolver[]{
                new ArrayELResolver(),
                new ListELResolver(),
                new MapELResolver(),
                new BeanELResolver(),
                new ServiceTaskResolver(serviceTaskRegistry)
        };
    }

    public DefaultExpressionManager(ELResolver... resolvers) {
        this.resolvers = resolvers;
    }

    private ELResolver createResolver(ExecutionContext ctx) {
        CompositeELResolver cr = new CompositeELResolver();
        for (ELResolver r : resolvers) {
            cr.add(r);
        }
        cr.add(new ExecutionContextVariableResolver(ctx));
        return cr;
    }

    @Override
    public <T> T eval(ExecutionContext ctx, String expr, Class<T> type) {
        try {
            ELResolver r = createResolver(ctx);
            SimpleContext sc = new SimpleContext(r);
            sc.setVariable("execution", expressionFactory.createValueExpression(ctx, ExecutionContext.class));
            sc.putContext(ExpressionFactory.class, expressionFactory);

            ValueExpression x = expressionFactory.createValueExpression(sc, expr, type);
            Object v = x.getValue(sc);

            return type.cast(v);
        } catch (Exception e) {
            log.warn("eval ['{}'] -> error: ", expr, e.getMessage());
            throw e;
        }
    }

    public static String quote(String s) {
        return s.replace("'", "\'");
    }
}
