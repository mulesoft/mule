/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.exceptions;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} to be thrown in the cases in which there is not enough permissions to access a particular file or
 * directory.
 *
 * @since 4.0
 */
public final class FileAccessDeniedException extends ModuleException {

  private static final FileError ERROR = FileError.ACCESS_DENIED;

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public FileAccessDeniedException(String message) {
    super(message, ERROR);
  }

  public FileAccessDeniedException(String message, Exception exception) {
    super(exception, ERROR, message);
  }
}
