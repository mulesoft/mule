/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
