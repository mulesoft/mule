/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.CLIENT_SECURITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.CONNECTIVITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.DUPLICATE_MESSAGE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.EXPRESSION;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.FATAL;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.OVERLOAD;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.RETRY_EXHAUSTED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.ROUTING;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SECURITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SERVER_SECURITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.STREAM_MAXIMUM_SIZE_EXCEEDED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.TRANSFORMATION;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.UNKNOWN;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.VALIDATION;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.security.ClientSecurityException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.ServerSecurityException;
import org.mule.runtime.api.streaming.exception.StreamingBufferSizeExceededException;
import org.mule.runtime.core.api.exception.ErrorTypeLocator;
import org.mule.runtime.core.api.exception.ErrorTypeRepository;
import org.mule.runtime.core.api.exception.ExceptionMapper;
import org.mule.runtime.core.api.exception.MessageRedeliveredException;
import org.mule.runtime.core.api.exception.MuleFatalException;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.routing.DuplicateMessageException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.routing.ValidationException;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;

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
    ErrorType unknown = errorTypeRepository.getErrorType(UNKNOWN).get();
    return ErrorTypeLocator.builder(errorTypeRepository)
        .defaultExceptionMapper(ExceptionMapper.builder()
            .addExceptionMapping(MessageTransformerException.class, errorTypeRepository.lookupErrorType(TRANSFORMATION).get())
            .addExceptionMapping(TransformerException.class, errorTypeRepository.lookupErrorType(TRANSFORMATION).get())
            .addExceptionMapping(ExpressionRuntimeException.class, errorTypeRepository.lookupErrorType(EXPRESSION).get())
            .addExceptionMapping(RoutingException.class, errorTypeRepository.lookupErrorType(ROUTING).get())
            .addExceptionMapping(ConnectionException.class, errorTypeRepository.lookupErrorType(CONNECTIVITY).get())
            .addExceptionMapping(ValidationException.class, errorTypeRepository.lookupErrorType(VALIDATION).get())
            .addExceptionMapping(DuplicateMessageException.class, errorTypeRepository.lookupErrorType(DUPLICATE_MESSAGE).get())
            .addExceptionMapping(RetryPolicyExhaustedException.class, errorTypeRepository.lookupErrorType(RETRY_EXHAUSTED).get())
            .addExceptionMapping(IOException.class, errorTypeRepository.lookupErrorType(CONNECTIVITY).get())
            .addExceptionMapping(SecurityException.class, errorTypeRepository.lookupErrorType(SECURITY).get())
            .addExceptionMapping(ClientSecurityException.class, errorTypeRepository.lookupErrorType(CLIENT_SECURITY).get())
            .addExceptionMapping(ServerSecurityException.class, errorTypeRepository.lookupErrorType(SERVER_SECURITY).get())
            .addExceptionMapping(RejectedExecutionException.class, errorTypeRepository.getErrorType(OVERLOAD).get())
            .addExceptionMapping(MessageRedeliveredException.class,
                                 errorTypeRepository.lookupErrorType(REDELIVERY_EXHAUSTED).get())
            .addExceptionMapping(Exception.class, unknown)
            .addExceptionMapping(Error.class, errorTypeRepository.getCriticalErrorType())
            .addExceptionMapping(StreamingBufferSizeExceededException.class,
                                 errorTypeRepository.lookupErrorType(STREAM_MAXIMUM_SIZE_EXCEEDED).get())
            .addExceptionMapping(MuleFatalException.class, errorTypeRepository.getErrorType(FATAL).get())
            .build())
        .defaultError(unknown)
        .build();
  }
}
