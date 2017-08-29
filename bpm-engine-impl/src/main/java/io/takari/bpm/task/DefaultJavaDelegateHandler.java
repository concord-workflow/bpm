package io.takari.bpm.task;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;

public class DefaultJavaDelegateHandler implements JavaDelegateHandler {

    @Override
    public void execute(Object task, ExecutionContext ctx) throws Exception {
        if (task instanceof JavaDelegate) {
            JavaDelegate d = (JavaDelegate) task;
            d.execute(ctx);
        } else {
            throw new ExecutionException("Unexpected result type: " + task + ". Was expecting an instance of JavaDelegate");
        }
    }
}
