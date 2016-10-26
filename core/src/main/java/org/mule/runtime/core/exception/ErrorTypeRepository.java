/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.lang.String.format;
import static org.mule.runtime.core.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.exception.Errors.Identifiers.ANY_IDENTIFIER;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.message.ErrorTypeBuilder;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for the different {@link ErrorType}s in a mule artifact.
 *
 * Only one instance of {@link ErrorType} must exists describing the same combination of error identifier and namespace.
 *
 * @since 4.0
 */
public class ErrorTypeRepository {

  protected static final ErrorType ANY_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(ANY_IDENTIFIER).build();

  private Map<ComponentIdentifier, ErrorType> errorTypes = new HashMap<>();

  public ErrorTypeRepository() {
    this.errorTypes.put(new ComponentIdentifier.Builder()
        .withNamespace(CORE_NAMESPACE_NAME)
        .withName(ANY_IDENTIFIER)
        .build(),
                        ANY_ERROR_TYPE);
  }

  public ErrorType addErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    ErrorTypeBuilder errorTypeBuilder =
        ErrorTypeBuilder.builder().namespace(errorTypeIdentifier.getNamespace())
            .identifier(errorTypeIdentifier.getName())
            .parentErrorType(parentErrorType);
    ErrorType errorType = errorTypeBuilder.build();
    if (this.errorTypes.put(errorTypeIdentifier, errorType) != null) {
      throw new IllegalStateException(format("An error type with identifier %s already exists", errorTypeIdentifier));
    }
    return errorType;
  }

  public ErrorType lookupErrorType(ComponentIdentifier errorTypeComponentIdentifier) {
    ErrorType errorType = this.errorTypes.get(errorTypeComponentIdentifier);
    if (errorType == null) {
      throw new IllegalStateException(format("there's no error type for %s", errorTypeComponentIdentifier));
    }
    return errorType;
  }

  /**
   * Gets the {@code ErrorType} instance for ANY error type.
   *
   * @return the ANY error type
   */
  public ErrorType getAnyErrorType() {
    return ANY_ERROR_TYPE;
  }

}
