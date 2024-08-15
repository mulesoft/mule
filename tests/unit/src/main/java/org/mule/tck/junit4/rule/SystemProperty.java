/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.rules.ExternalResource;

/**
 * Sets up a system property before a test and guarantees to tear it down afterward.
 */
public class SystemProperty extends ExternalResource {

  /**
   * Utility method to execute a callable with a system property and restoring the property's original value afterwards.
   *
   * @return the return value of the callable.
   */
  public static <V> V callWithProperty(String name, String value, Callable<V> callable) throws Throwable {
    SystemProperty property = new SystemProperty(name, value);
    property.before();
    try {
      return callable.call();
    } catch (Exception e) {
      throw new Exception("Callable threw an exception during its execution.", e);
    } finally {
      property.after();
    }
  }

  protected boolean initialized;
  protected String value;

  private final String name;
  private String oldValue;
  private static final Map<String, Lock> propertyAccessLock = new ConcurrentHashMap<>();

  public SystemProperty(String name) {
    this(name, null);
  }

  public SystemProperty(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  protected void before() throws Throwable {
    if (initialized) {
      throw new IllegalArgumentException("System property was already initialized");
    }

    // System properties are global properties.
    // Avoid collisions with other rules running in parallel.
    propertyAccessLock.computeIfAbsent(name, s -> new ReentrantLock()).lock();

    if (getValue() == null) {
      oldValue = System.clearProperty(name);
    } else {
      oldValue = System.setProperty(name, getValue());
    }
    initialized = true;
  }

  @Override
  protected void after() {
    if (!initialized) {
      throw new IllegalArgumentException("System property was not initialized");
    }

    doCleanUp();
    restoreOldValue();

    initialized = false;

    propertyAccessLock.get(name).unlock();
  }

  protected void restoreOldValue() {
    if (oldValue == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, oldValue);
    }
  }

  public String getName() {
    return name;
  }

  protected void doCleanUp() {
    // Nothing to do
  }

  public String getValue() {
    return value;
  }
}
