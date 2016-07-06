package io.takari.bpm.testkit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Deployment {
  
  public String[] resources() default {};
}
