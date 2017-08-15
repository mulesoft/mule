/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.CLIENT_SECURITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.COMPOSITE_ROUTING;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.CONNECTIVITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.DUPLICATE_MESSAGE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.EXPRESSION;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.FATAL;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.NOT_PERMITTED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.OVERLOAD;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.RETRY_EXHAUSTED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.ROUTING;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SECURITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SERVER_SECURITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.STREAM_MAXIMUM_SIZE_EXCEEDED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.TIMEOUT;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.TRANSFORMATION;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.VALIDATION;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.exception.CompositeErrorTypeRepository;
import org.mule.runtime.core.api.exception.DefaultErrorTypeRepository;
import org.mule.runtime.core.api.exception.ErrorTypeRepository;

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
    ErrorTypeRepository errorTypeRepository = new DefaultErrorTypeRepository();
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
    errorTypeRepository.addErrorType(NOT_PERMITTED, errorTypeRepository.getErrorType(SERVER_SECURITY).get());
    errorTypeRepository.addInternalErrorType(OVERLOAD, errorTypeRepository.getCriticalErrorType());
    errorTypeRepository.addErrorType(STREAM_MAXIMUM_SIZE_EXCEEDED, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addInternalErrorType(FATAL, errorTypeRepository.getCriticalErrorType());
    errorTypeRepository.addErrorType(TIMEOUT, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(COMPOSITE_ROUTING, errorTypeRepository.getErrorType(ROUTING).get());

    final ErrorType sourceErrorType = errorTypeRepository.getSourceErrorType();
    errorTypeRepository.addErrorType(SOURCE_RESPONSE_GENERATE, errorTypeRepository.getSourceResponseErrorType());
    errorTypeRepository.addErrorType(SOURCE_RESPONSE_SEND, errorTypeRepository.getSourceResponseErrorType());
    errorTypeRepository.addInternalErrorType(SOURCE_ERROR_RESPONSE_GENERATE, sourceErrorType);
    errorTypeRepository.addInternalErrorType(SOURCE_ERROR_RESPONSE_SEND, sourceErrorType);

    return errorTypeRepository;
  }

  /**
   * Creates a new {@link CompositeErrorTypeRepository} to use in mule.
   * <p>
   * The created repository will have a {@link ErrorTypeRepository} as child created by {@link ErrorTypeRepositoryFactory#createDefaultErrorTypeRepository()}
   * and the given {@code parentErrorTypeRepository} as parent.
   *
   * @param parentErrorTypeRepository {@link ErrorTypeRepository} to be used as the parent repository
   * @return a new {@link CompositeErrorTypeRepository} with the given {@code parentErrorTypeRepository} as the parent
   * repository
   */
  public static ErrorTypeRepository createCompositeErrorTypeRepository(ErrorTypeRepository parentErrorTypeRepository) {
    checkNotNull(parentErrorTypeRepository, "'parentErrorTypeRepository' can't be null");

    return new CompositeErrorTypeRepository(createDefaultErrorTypeRepository(), parentErrorTypeRepository);
  }
}
