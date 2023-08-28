/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime;

/**
 * Generic exception for when the resolution of a value fails
 * 
 * @since 4.0
 */
public class ValueResolvingException extends Exception {

  public ValueResolvingException() {
    super();
  }

  public ValueResolvingException(String message) {
    super(message);
  }

  public ValueResolvingException(String message, Throwable cause) {
    super(message, cause);
  }
}
