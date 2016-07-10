package io.takari.bpm.el;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ValueExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.task.ServiceTaskRegistry;
import io.takari.bpm.task.ServiceTaskResolver;

public class DefaultExpressionManager implements ExpressionManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultExpressionManager.class);

    private final ExpressionFactory expressionFactory = new ExpressionFactoryImpl();
    private final ELResolver[] resolvers;

    public DefaultExpressionManager(ServiceTaskRegistry serviceTaskRegistry) {
        resolvers = new ELResolver[] {
                new ArrayELResolver(),
                new ListELResolver(),
                new MapELResolver(),
                new BeanELResolver(),
                new ServiceTaskResolver(serviceTaskRegistry)
        };
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
            log.error("eval ['{}'] -> error", expr, e);
            throw e;
        }
    }
}
