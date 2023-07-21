/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.junit4.rule;

import static org.mule.runtime.api.exception.MuleException.MULE_VERBOSE_EXCEPTIONS;
import static org.mule.runtime.api.exception.MuleException.refreshVerboseExceptions;

import org.junit.rules.ExternalResource;

/**
 * Sets or unsets the {@code verboseExceptions} flag for a test.
 */
public class VerboseExceptions extends ExternalResource {

  public SystemProperty verboseExceptions;

  public VerboseExceptions(boolean verbose) {
    verboseExceptions = new SystemProperty(MULE_VERBOSE_EXCEPTIONS, Boolean.toString(verbose));
  }

  @Override
  protected void before() throws Throwable {
    super.before();
    verboseExceptions.before();
    refreshVerboseExceptions();
  }

  @Override
  protected void after() {
    super.after();
    verboseExceptions.after();
    refreshVerboseExceptions();
  }

  public static void setVerboseExceptions(boolean verbose) {
    System.setProperty(MULE_VERBOSE_EXCEPTIONS, Boolean.toString(verbose));
    refreshVerboseExceptions();
  }

  @Override
  public String toString() {
    return "VerboseExceptions: " + verboseExceptions.getValue();
  }
}
