/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

import org.junit.rules.ExternalResource;

/**
 * Sets up an environment variable before a test and guarantees to tear it down afterward.
 */
public class EnvironmentVariable extends ExternalResource {

  protected boolean initialized;
  protected String value;

  private final String name;
  private String oldValue;

  public EnvironmentVariable(String name) {
    this(name, null);
  }

  public EnvironmentVariable(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  protected void before() throws Throwable {
    if (initialized) {
      throw new IllegalArgumentException("Environment variable was already initialized");
    }

    if (getValue() == null) {
      oldValue = clearEnvironmentVariable(name);
    } else {
      oldValue = updateEnvironmentVariable(name, getValue());
    }
    initialized = true;
  }

  @Override
  protected void after() {
    if (!initialized) {
      throw new IllegalArgumentException("Environment variable was not initialized");
    }

    doCleanUp();
    restoreOldValue();

    initialized = false;
  }

  protected void restoreOldValue() {
    if (oldValue == null) {
      clearEnvironmentVariable(name);
    } else {
      updateEnvironmentVariable(name, oldValue);
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

  public String clearEnvironmentVariable(String name) {
    return updateEnvironmentVariablesMap(m -> m.remove(name));
  }

  public String updateEnvironmentVariable(String name, String val) {
    return updateEnvironmentVariablesMap(m -> m.put(name, val));
  }

  private String updateEnvironmentVariablesMap(Function<Map<String, String>, String> f) {
    Map<String, String> env = System.getenv();
    Field field = null;
    try {
      field = env.getClass().getDeclaredField("m");
      field.setAccessible(true);
      return f.apply(((Map<String, String>) field.get(env)));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Unable to add environment variable", e);
    } finally {
      if (field != null) {
        field.setAccessible(true);
      }
    }
  }
}
