/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.span.validation;

/**
 * An exception indicating that a condition was not met on a tracing operation.
 *
 * @since 4.5.0
 */
public class AssertionFailedException
    extends RuntimeException {

  public AssertionFailedException(String message) {
    super(message);
  }

}
