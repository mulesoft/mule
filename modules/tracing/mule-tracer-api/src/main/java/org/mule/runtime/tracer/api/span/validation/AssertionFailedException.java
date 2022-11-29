/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
