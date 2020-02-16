/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
