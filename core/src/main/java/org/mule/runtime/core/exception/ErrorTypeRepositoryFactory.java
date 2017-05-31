/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.CLIENT_SECURITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.CONNECTIVITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.DUPLICATE_MESSAGE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.EXPRESSION;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.FATAL;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.OVERLOAD;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.RETRY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.ROUTING;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SECURITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SERVER_SECURITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.STREAM_MAXIMUM_SIZE_EXCEEDED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.TRANSFORMATION;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.VALIDATION;
import org.mule.runtime.api.message.ErrorType;

/**
 * Factory for {@link ErrorTypeRepository}.
 * 
 * @since 4.0
 */
public class ErrorTypeRepositoryFactory {

  /**
   * Creates the default {@link ErrorTypeRepository} to use in mule.
   * <p>
   * The {@link ErrorTypeRepository} gets populated with the default mappings between common core exceptions and core error types.
   * 
   * @return a new {@link ErrorTypeRepository}.
   */
  public static ErrorTypeRepository createDefaultErrorTypeRepository() {
    ErrorTypeRepository errorTypeRepository = new ErrorTypeRepository();
    errorTypeRepository.addErrorType(TRANSFORMATION, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(EXPRESSION, errorTypeRepository.getAnyErrorType());
    final ErrorType validationErrorType = errorTypeRepository.addErrorType(VALIDATION, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(DUPLICATE_MESSAGE, validationErrorType);
    errorTypeRepository.addErrorType(REDELIVERY_EXHAUSTED, errorTypeRepository.getAnyErrorType());
    final ErrorType connectivityErrorType = errorTypeRepository.addErrorType(CONNECTIVITY, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(RETRY_EXHAUSTED, connectivityErrorType);
    errorTypeRepository.addErrorType(ROUTING, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(SECURITY, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(CLIENT_SECURITY, errorTypeRepository.getErrorType(SECURITY).get());
    errorTypeRepository.addErrorType(SERVER_SECURITY, errorTypeRepository.getErrorType(SECURITY).get());
    errorTypeRepository.addInternalErrorType(OVERLOAD, errorTypeRepository.getCriticalErrorType());
    errorTypeRepository.addErrorType(STREAM_MAXIMUM_SIZE_EXCEEDED, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addInternalErrorType(FATAL, errorTypeRepository.getCriticalErrorType());

    final ErrorType sourceErrorType = errorTypeRepository.getSourceErrorType();
    errorTypeRepository.addErrorType(SOURCE_RESPONSE_GENERATE, errorTypeRepository.getSourceResponseErrorType());
    errorTypeRepository.addErrorType(SOURCE_RESPONSE_SEND, errorTypeRepository.getSourceResponseErrorType());
    errorTypeRepository.addErrorType(SOURCE_ERROR_RESPONSE_GENERATE, sourceErrorType);
    errorTypeRepository.addErrorType(SOURCE_ERROR_RESPONSE_SEND, sourceErrorType);

    return errorTypeRepository;
  }

}
