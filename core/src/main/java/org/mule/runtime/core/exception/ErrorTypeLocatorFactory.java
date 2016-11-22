/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.ErrorTypeRepository.ANY_ERROR_TYPE;
import static org.mule.runtime.core.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.CONNECTIVITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.EXPRESSION;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.OVERLOAD;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.RETRY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.ROUTING;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SECURITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.TRANSFORMATION;
import static org.mule.runtime.core.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.ErrorTypeBuilder;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;

/**
 * Factory for {@link ErrorTypeLocator}.
 * 
 * @since 4.0
 */
public class ErrorTypeLocatorFactory {

  /**
   * Error type for which there's no clear reason for failure. Will be used when no specific match is found.
   */
  private static final ErrorType UNKNOWN_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(UNKNOWN_ERROR_IDENTIFIER)
          .parentErrorType(ANY_ERROR_TYPE).build();

  /**
   * Error type for which there will be no handling since it represents an error so critical it should not be handled.
   * If such an error occurs it will always be propagated.
   */
  public static final ErrorType CRITICAL_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(CRITICAL_IDENTIFIER)
          .parentErrorType(null).build();

  /**
   * Creates the default {@link ErrorTypeLocator} to use in mule.
   * 
   * @param errorTypeRepository error type repository. Commonly created using {@link ErrorTypeRepositoryFactory}.
   * @return a new {@link ErrorTypeLocator}.
   */
  public static ErrorTypeLocator createDefaultErrorTypeLocator(ErrorTypeRepository errorTypeRepository) {
    return ErrorTypeLocator.builder(errorTypeRepository)
        .defaultExceptionMapper(ExceptionMapper.builder()
            .addExceptionMapping(MessageTransformerException.class, errorTypeRepository.lookupErrorType(TRANSFORMATION))
            .addExceptionMapping(TransformerException.class, errorTypeRepository.lookupErrorType(TRANSFORMATION))
            .addExceptionMapping(ExpressionRuntimeException.class, errorTypeRepository.lookupErrorType(EXPRESSION))
            .addExceptionMapping(RoutingException.class, errorTypeRepository.lookupErrorType(ROUTING))
            .addExceptionMapping(ConnectionException.class, errorTypeRepository.lookupErrorType(CONNECTIVITY))
            .addExceptionMapping(RetryPolicyExhaustedException.class, errorTypeRepository.lookupErrorType(RETRY_EXHAUSTED))
            .addExceptionMapping(IOException.class, errorTypeRepository.lookupErrorType(CONNECTIVITY))
            .addExceptionMapping(SecurityException.class, errorTypeRepository.lookupErrorType(SECURITY))
            .addExceptionMapping(RejectedExecutionException.class, errorTypeRepository.lookupErrorType(OVERLOAD))
            .addExceptionMapping(MessageRedeliveredException.class, errorTypeRepository.lookupErrorType(REDELIVERY_EXHAUSTED))
            .addExceptionMapping(Exception.class, UNKNOWN_ERROR_TYPE)
            .addExceptionMapping(Error.class, CRITICAL_ERROR_TYPE)
            .build())
        .build();
  }

}
