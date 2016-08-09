/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
