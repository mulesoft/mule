/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

/**
 * Determines that an error thrown by an operation should be mapped to another.
 */
public final class ErrorMapping {

  private final String source;
  private final String target;

  public ErrorMapping(String source, String target) {
    this.source = source;
    this.target = target;
  }

  /**
   * @return the type of the error to be mapped from
   */
  public String getSource() {
    return source;
  }

  /**
   * @return the type of the error to be mapped to
   */
  public String getTarget() {
    return target;
  }

  @Override
  public String toString() {
    return "ErrorMapping: " + source + " -> " + target;
  }
}
