/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.exceptions;

import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} to be thrown in the cases in which a given file already exists. For example, when trying to create a
 * new file with a {@link FileWriteMode#CREATE_NEW} write mode and the file already existed.
 *
 * @since 4.0
 */
public final class FileAlreadyExistsException extends ModuleException {

  private static final FileError ERROR = FileError.FILE_ALREADY_EXISTS;

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public FileAlreadyExistsException(String message) {
    super(message, ERROR);
  }

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   * @param exception cause of this exception
   */
  public FileAlreadyExistsException(String message, Exception exception) {
    super(exception, ERROR, message);
  }
}
