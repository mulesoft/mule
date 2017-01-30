/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.exceptions;


import static java.util.Optional.ofNullable;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

/**
 * Errors for the file family extensions
 * 
 * @since 4.0
 */
public enum FileError implements ErrorTypeDefinition<FileError> {

  FILE_NOT_FOUND,

  ILLEGAL_PATH,

  ILLEGAL_CONTENT,

  FILE_LOCK,

  FILE_ALREADY_EXISTS,

  ACCESS_DENIED,

  CONNECTIVITY(MuleErrors.CONNECTIVITY),

  FILE_DOESNT_EXIST(CONNECTIVITY),

  FILE_IS_NOT_DIRECTORY(CONNECTIVITY),

  INVALID_CREDENTIALS(CONNECTIVITY),

  CONNECTION_TIMEOUT(CONNECTIVITY),

  CANNOT_REACH(CONNECTIVITY),

  UNKNOWN_HOST(CONNECTIVITY),

  SERVICE_NOT_AVAILABLE(CONNECTIVITY),

  DISCONNECTED(CONNECTIVITY);

  private ErrorTypeDefinition<? extends Enum<?>> parentError;

  FileError(ErrorTypeDefinition<? extends Enum<?>> parentError) {
    this.parentError = parentError;
  }

  FileError() {

  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parentError);
  }
}
