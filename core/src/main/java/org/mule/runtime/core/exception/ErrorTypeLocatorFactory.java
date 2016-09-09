/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.CONNECTIVITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.EXPRESSION;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.RETRY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.ROUTING;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SECURITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.TRANSFORMATION;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.UNKNOWN;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;

import java.io.IOException;

/**
 * Factory for {@link ErrorTypeLocator}.
 * 
 * @since 4.0
 */
public class ErrorTypeLocatorFactory {

  /**
   * Creates the default {@link ErrorTypeLocator} to use in mule.
   * 
   * @param errorTypeRepository error type repository. Commonly created using {@link ErrorTypeRepositoryFactory}.
   * @return a new {@link ErrorTypeLocatorFactory}.
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
            .addExceptionMapping(MessageRedeliveredException.class, errorTypeRepository.lookupErrorType(REDELIVERY_EXHAUSTED))
            .addExceptionMapping(Exception.class, errorTypeRepository.lookupErrorType(UNKNOWN))
            .build())
        .build();
  }

}
