/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Specifies the {@link Runner} that {@link ArtifactClassLoaderRunner} delegates to.
 *
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RunnerDelegateTo {

  /**
   * @return the {@link Runner} that would be used to delegate the execution of the test.
   */
  Class<? extends Runner> value() default BlockJUnit4ClassRunner.class;

}
