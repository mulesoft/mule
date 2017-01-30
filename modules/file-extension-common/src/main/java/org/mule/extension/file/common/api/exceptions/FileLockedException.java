/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.exceptions;

import static org.mule.extension.file.common.api.exceptions.FileError.*;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} for the cases in which a lock cannot be acquired over a file.
 * 
 * @since 4.0
 */
public final class FileLockedException extends ModuleException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public FileLockedException(String message) {
    super(message, FILE_LOCK);
  }
}

