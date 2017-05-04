/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.CONNECTIVITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.EXPRESSION;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.FILTERED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.OVERLOAD;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.RETRY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.ROUTING;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SECURITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.STREAM_MAXIMUM_SIZE_EXCEEDED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.TRANSFORMATION;

import org.mule.runtime.api.message.ErrorType;

/**
 * Factory for {@link ErrorTypeRepository}.
 * 
 * @since 4.0
 */
public class ErrorTypeRepositoryFactory {

  /**
   * Creates the default {@link ErrorTypeRepository} to use in mule.
   * 
   * The {@link ErrorTypeRepository} gets populated with the default mappings between common core exceptions and core error types.
   * 
   * @return a new {@link ErrorTypeRepository}.
   */
  public static ErrorTypeRepository createDefaultErrorTypeRepository() {
    ErrorTypeRepository errorTypeRepository = new ErrorTypeRepository();
    errorTypeRepository.addErrorType(TRANSFORMATION, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(EXPRESSION, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(FILTERED, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(REDELIVERY_EXHAUSTED, errorTypeRepository.getAnyErrorType());
    ErrorType connectivityErrorType = errorTypeRepository.addErrorType(CONNECTIVITY, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(RETRY_EXHAUSTED, connectivityErrorType);
    errorTypeRepository.addErrorType(ROUTING, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(SECURITY, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addInternalErrorType(OVERLOAD, errorTypeRepository.getCriticalErrorType());
    errorTypeRepository.addErrorType(STREAM_MAXIMUM_SIZE_EXCEEDED, errorTypeRepository.getAnyErrorType());
    return errorTypeRepository;
  }

}
