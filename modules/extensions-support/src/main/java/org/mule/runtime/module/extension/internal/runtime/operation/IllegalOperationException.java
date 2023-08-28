/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.module.extension.internal.runtime.exception.IllegalComponentException;

/**
 * A {@link IllegalComponentException} which marks that a current operation is not valid or is misconfigured
 *
 * @since 4.0
 */
public class IllegalOperationException extends IllegalComponentException {

  public IllegalOperationException(String message) {
    super(message);
  }

  public IllegalOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
