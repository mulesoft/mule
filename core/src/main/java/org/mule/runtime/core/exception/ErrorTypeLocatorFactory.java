/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.CONNECTIVITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.EXPRESSION;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.OVERLOAD;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.RETRY_EXHAUSTED;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.ROUTING;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SECURITY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.TRANSFORMATION;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.UNKNOWN;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.scheduler.SchedulerBusyException;
import org.mule.runtime.api.security.SecurityException;
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
   * @return a new {@link ErrorTypeLocator}.
   */
  public static ErrorTypeLocator createDefaultErrorTypeLocator(ErrorTypeRepository errorTypeRepository) {
    return ErrorTypeLocator.builder(errorTypeRepository)
        .defaultExceptionMapper(ExceptionMapper.builder()
            .addExceptionMapping(MessageTransformerException.class, errorTypeRepository.lookupErrorType(TRANSFORMATION).get())
            .addExceptionMapping(TransformerException.class, errorTypeRepository.lookupErrorType(TRANSFORMATION).get())
            .addExceptionMapping(ExpressionRuntimeException.class, errorTypeRepository.lookupErrorType(EXPRESSION).get())
            .addExceptionMapping(RoutingException.class, errorTypeRepository.lookupErrorType(ROUTING).get())
            .addExceptionMapping(ConnectionException.class, errorTypeRepository.lookupErrorType(CONNECTIVITY).get())
            .addExceptionMapping(RetryPolicyExhaustedException.class, errorTypeRepository.lookupErrorType(RETRY_EXHAUSTED).get())
            .addExceptionMapping(IOException.class, errorTypeRepository.lookupErrorType(CONNECTIVITY).get())
            .addExceptionMapping(SecurityException.class, errorTypeRepository.lookupErrorType(SECURITY).get())
            .addExceptionMapping(SchedulerBusyException.class, errorTypeRepository.getErrorType(OVERLOAD).get())
            .addExceptionMapping(MessageRedeliveredException.class,
                                 errorTypeRepository.lookupErrorType(REDELIVERY_EXHAUSTED).get())
            .addExceptionMapping(Exception.class, errorTypeRepository.getErrorType(UNKNOWN).get())
            .addExceptionMapping(Error.class, errorTypeRepository.getCriticalErrorType())
            .build())
        .build();
  }

}
