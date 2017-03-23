/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.iterator;

/**
 * Exception to signal that a consumer you're trying to access is already closed
 * 
 * @since 3.5.0
 */
public class ClosedConsumerException extends RuntimeException {

  private static final long serialVersionUID = -342147990165817320L;

  public ClosedConsumerException() {
    super();
  }

  public ClosedConsumerException(String message) {
    super(message);
  }
}
