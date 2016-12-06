/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import static org.mule.extension.validation.api.ValidationErrorTypes.VALIDATION;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricher;

/**
 * {@link ExceptionEnricher} implementation for {@link ValidationException} to wrap them and throw
 * an {@link ModuleException} specifying the correspondent {@link ErrorTypeDefinition}
 *
 * @since 4.0
 */
public class ValidationExceptionEnricher implements ExceptionEnricher {

  @Override
  public Exception enrichException(Exception e) {
    if (e instanceof ValidationException) {
      return new ModuleException(e, VALIDATION);
    }
    return e;
  }
}
