/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.ErrorTypeRepository.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.exception.ErrorTypeRepository.EXPRESSION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.ErrorTypeRepository.REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.ErrorTypeRepository.TRANSFORMATION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.ErrorTypeRepository.UNKNOWN_ERROR_IDENTIFIER;

import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.transformer.TransformerMessagingException;
import org.mule.runtime.core.config.ComponentIdentifier;

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
            .addExceptionMapping(TransformerMessagingException.class,
                                 errorTypeRepository.lookupErrorType(new ComponentIdentifier.Builder()
                                     .withNamespace(CORE_NAMESPACE_NAME).withName(TRANSFORMATION_ERROR_IDENTIFIER).build()))
            .addExceptionMapping(ExpressionRuntimeException.class,
                                 errorTypeRepository.lookupErrorType(new ComponentIdentifier.Builder()
                                     .withNamespace(CORE_NAMESPACE_NAME).withName(EXPRESSION_ERROR_IDENTIFIER).build()))
            .addExceptionMapping(MessageRedeliveredException.class,
                                 errorTypeRepository.lookupErrorType(new ComponentIdentifier.Builder()
                                     .withNamespace(CORE_NAMESPACE_NAME).withName(REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER).build()))
            .addExceptionMapping(Exception.class,
                                 errorTypeRepository.lookupErrorType(new ComponentIdentifier.Builder()
                                     .withNamespace(CORE_NAMESPACE_NAME).withName(UNKNOWN_ERROR_IDENTIFIER).build()))
            .build())
        .build();
  }

}
