/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.ErrorTypeRepository.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.exception.ErrorTypeRepository.EXPRESSION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.ErrorTypeRepository.TRANSFORMATION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.ErrorTypeRepository.UNKNOWN_ERROR_IDENTIFIER;

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
    errorTypeRepository.addErrorType(CORE_NAMESPACE_NAME, TRANSFORMATION_ERROR_IDENTIFIER, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(CORE_NAMESPACE_NAME, EXPRESSION_ERROR_IDENTIFIER, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(CORE_NAMESPACE_NAME, UNKNOWN_ERROR_IDENTIFIER, errorTypeRepository.getAnyErrorType());
    return errorTypeRepository;
  }

}
