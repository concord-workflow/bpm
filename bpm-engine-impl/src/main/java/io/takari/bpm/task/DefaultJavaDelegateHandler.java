package io.takari.bpm.task;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.JavaDelegate;

public class DefaultJavaDelegateHandler implements JavaDelegateHandler {

    @Override
    public void execute(Object task, ExecutionContext ctx) throws Exception {
        JavaDelegate d = (JavaDelegate) task;
        d.execute(ctx);
    }
}
