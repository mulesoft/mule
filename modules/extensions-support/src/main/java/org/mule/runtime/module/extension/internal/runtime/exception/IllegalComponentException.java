/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

/**
 * A {@link RuntimeException} which marks that a selected component is not valid or is misconfigured
 *
 * @since 4.0
 */
public class IllegalComponentException extends RuntimeException {

  public IllegalComponentException(String message) {
    super(message);
  }

  public IllegalComponentException(String message, Throwable cause) {
    super(message, cause);
  }
}
