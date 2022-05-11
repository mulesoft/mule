/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import static java.lang.Class.forName;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.rules.ExternalResource;

/**
 * Cleans up resources stored by slf4j-test library used for testing logging behaviour
 */
public class LogCleanup extends ExternalResource {

  private static Method clearAllMethod;

  @Override
  protected void before() throws Throwable {
    clearAllLogs();
  }

  @Override
  protected void after() {
    clearAllLogs();
  }

  /**
   * Logs that are stored for later assert need to be cleared before every test
   * <p>
   * Reflection needs to be used because slf4j-test is not included on every module
   */
  public static void clearAllLogs() {
    try {
      // Loading resources once per class for better performance
      if (clearAllMethod == null) {
        Class<?> testLoggerFactoryClass =
            forName("com.github.valfirst.slf4jtest.TestLoggerFactory", false, LogCleanup.class.getClassLoader());
        clearAllMethod = testLoggerFactoryClass.getMethod("clearAll");
      }
      // clearAll will reset state across all threads
      clearAllMethod.invoke(null);
    } catch (ClassNotFoundException ignored) {
      // In this case, the class was not loaded because it does not exist in the current classpath so the method must finish
    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException("Could not clear TestLoggerFactory logs", e);
    }
  }
}
