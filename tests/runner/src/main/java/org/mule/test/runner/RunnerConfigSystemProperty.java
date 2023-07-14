/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

/**
 * Specifies a system property to be set only during the creation of a classloader running (and the loading of the extension
 * models it does).
 * <p>
 * A simple JUnit rule cannot be used for this because JUnit applies the rules AFTER the test runner has been configured.
 * 
 * @since 4.5
 */
@Retention(RUNTIME)
public @interface RunnerConfigSystemProperty {

  /**
   * The system property to set.
   */
  String key();

  /**
   * The value to set on the system property.
   */
  String value();
}
