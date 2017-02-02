/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.exceptions;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} to be thrown in the cases in which a given path is invalid. For instance, if the path is {@code null}
 * or doesn't exist.
 *
 * @since 4.0
 */
public final class IllegalPathException extends ModuleException {

  private static final FileError ERROR = FileError.ILLEGAL_PATH;

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public IllegalPathException(String message) {
    super(message, ERROR);
  }

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   * @param exception cause of this exception
   */
  public IllegalPathException(String message, Exception exception) {
    super(exception, ERROR, message);
  }
}
