/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
