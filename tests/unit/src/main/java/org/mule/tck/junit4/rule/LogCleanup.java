/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import static java.lang.Class.forName;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.rules.ExternalResource;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/**
 * Cleans up resources stored by slf4j-test library used for testing logging behaviour
 */
public class LogCleanup extends ExternalResource {

  private static Method clearAllMethod;
  private static Object testMDCThreadLocals;
  private static Method resetMethod;

  @Override
  protected void before() throws Throwable {
    clearAllLogs();
  }

  @Override
  protected void after() {
    clearLogsAndMDCThreadReferences();
  }

  /**
   * Logs that are stored for later assert need to be cleared before every test
   * 
   * Reflection needs to be used because slf4j-test is not included on every module
   */
  public static void clearAllLogs() {
    try {
      // Loading resources once per class for better performance
      if (clearAllMethod == null) {
        Class<?> testLoggerFactoryClass =
            forName("uk.org.lidalia.slf4jtest.TestLoggerFactory", false, LogCleanup.class.getClassLoader());
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

  /**
   * TestMDCAdapter contains its own implementation of ThreadLocal variables which hold strong references to Threads that should
   * be released to prevent possible leakages
   * 
   * Reflection needs to be used because slf4j-test is not included on every module
   */
  public static void clearLogsAndMDCThreadReferences() {
    clearAllLogs();
    // Loading resources once per class for better performance
    if (testMDCThreadLocals == null || resetMethod == null) {
      Class<?> adapterClass;
      Class<?> threadLocalClass;
      try {
        adapterClass = forName("uk.org.lidalia.slf4jtest.TestMDCAdapter", false, LogCleanup.class.getClassLoader());
        threadLocalClass = forName("uk.org.lidalia.lang.ThreadLocal", false, LogCleanup.class.getClassLoader());
      } catch (ClassNotFoundException e) {
        // In this case, the class was not loaded because it does not exist in the current classpath so the method must finish
        return;
      }
      MDCAdapter testMDCAdapter = MDC.getMDCAdapter();
      try {
        Field valueField = adapterClass.getDeclaredField("value");
        valueField.setAccessible(true);
        testMDCThreadLocals = valueField.get(testMDCAdapter);
        resetMethod = threadLocalClass.getMethod("reset");
      } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
        throw new RuntimeException("Could not reset ThreadLocals", e);
      }
    }
    try {
      resetMethod.invoke(testMDCThreadLocals);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Could not reset ThreadLocals", e);
    }
  }
}
