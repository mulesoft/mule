/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.junit4;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a test method or class is flaky
 */
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface FlakyTest {

  /**
   * Indicate the number of times a flaky test must be executed.
   */
  int times() default 50;
}
