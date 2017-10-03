/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import org.junit.rules.ExternalResource;

/**
 * Sets up a system property before a test and guaranties to tear it down afterward.
 */
public class SystemProperty extends ExternalResource {

  protected boolean initialized;
  protected String value;

  private final String name;
  private String oldValue;

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
