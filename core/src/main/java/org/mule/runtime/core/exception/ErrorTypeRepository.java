/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.ANY;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.CRITICAL;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.UNKNOWN;
import static org.mule.runtime.core.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.SOURCE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.SOURCE_RESPONSE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for the different {@link ErrorType}s in a mule artifact.
 *
 * Only one instance of {@link ErrorType} must exists describing the same combination of error identifier and namespace.
 *
 * @since 4.0
 */
public class ErrorTypeRepository {

  /**
   * Error type that represents all of them that can be handled.
   */
  protected static final ErrorType ANY_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(ANY_IDENTIFIER).build();

  /**
   * Parent error type for errors that occur on the source of a flow.
   */
  protected static final ErrorType SOURCE_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(SOURCE_ERROR_IDENTIFIER)
          .parentErrorType(ANY_ERROR_TYPE).build();

  /**
   * Parent error type for errors that occur in a source processing a successful response of a flow.
   */
  protected static final ErrorType SOURCE_RESPONSE_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(SOURCE_RESPONSE_ERROR_IDENTIFIER)
          .parentErrorType(SOURCE_ERROR_TYPE).build();

  /**
   * Error type for which there's no clear reason for failure. Will be used when no specific match is found.
   */
  protected static final ErrorType UNKNOWN_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(UNKNOWN_ERROR_IDENTIFIER)
          .parentErrorType(ANY_ERROR_TYPE).build();

  /**
   * Error type for which there will be no handling since it represents an error so critical it should not be handled. If such an
   * error occurs it will always be propagated. Same for it's children.
   */
  protected static final ErrorType CRITICAL_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(CRITICAL_IDENTIFIER)
          .parentErrorType(null).build();

  private Map<ComponentIdentifier, ErrorType> errorTypes = new HashMap<>();
  private Map<ComponentIdentifier, ErrorType> internalErrorTypes = new HashMap<>();

  public ErrorTypeRepository() {
    this.errorTypes.put(ANY, ANY_ERROR_TYPE);
    this.errorTypes.put(SOURCE, SOURCE_ERROR_TYPE);
    this.errorTypes.put(SOURCE_RESPONSE, SOURCE_RESPONSE_ERROR_TYPE);
    this.internalErrorTypes.put(CRITICAL, CRITICAL_ERROR_TYPE);
    this.internalErrorTypes.put(UNKNOWN, UNKNOWN_ERROR_TYPE);
  }

  /**
   * Adds and returns an {@link ErrorType} for a given identifier with the given parent that will be fully visible, meaning it
   * will be available for use in on-error components.
   *
   * @param errorTypeIdentifier the {@link ComponentIdentifier} for the error
   * @param parentErrorType the {@link ErrorType} that will act as parent
   * @return the created {@link ErrorType}
   */
  public ErrorType addErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    return addErrorTypeTo(errorTypeIdentifier, parentErrorType, this.errorTypes);
  }

  /**
   * Adds and returns an {@link ErrorType} for a given identifier with the given parent that will be only used internally, meaning
   * it won't be available for use in on-error components.
   *
   * @param errorTypeIdentifier the {@link ComponentIdentifier} for the error
   * @param parentErrorType the {@link ErrorType} that will act as parent
   * @return the created {@link ErrorType}
   */
  public ErrorType addInternalErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    return addErrorTypeTo(errorTypeIdentifier, parentErrorType, this.internalErrorTypes);
  }

  private ErrorType addErrorTypeTo(ComponentIdentifier identifier, ErrorType parent, Map<ComponentIdentifier, ErrorType> map) {
    ErrorTypeBuilder errorTypeBuilder =
        ErrorTypeBuilder.builder().namespace(identifier.getNamespace())
            .identifier(identifier.getName())
            .parentErrorType(parent);
    ErrorType errorType = errorTypeBuilder.build();
    if (map.put(identifier, errorType) != null) {
      throw new IllegalStateException(format("An error type with identifier %s already exists", identifier));
    }
    return errorType;
  }

  /**
   * Looks up the specified error's type and returns it if found and available for general use (error handling).
   *
   * @param errorTypeComponentIdentifier the {@link ComponentIdentifier} for the error
   * @return an {@link Optional} with the corresponding {@link ErrorType} or an empty one
   */
  public Optional<ErrorType> lookupErrorType(ComponentIdentifier errorTypeComponentIdentifier) {
    return ofNullable(this.errorTypes.get(errorTypeComponentIdentifier));
  }

  /**
   * Returns the specified error's type if present. Unlike {@link #lookupErrorType(ComponentIdentifier)}, this will return the
   * {@link ErrorType} even if it's not available for general use (error handling).
   *
   * @param errorTypeIdentifier the {@link ComponentIdentifier} for the error
   * @return an {@link Optional} with the corresponding {@link ErrorType} or an empty one
   */
  public Optional<ErrorType> getErrorType(ComponentIdentifier errorTypeIdentifier) {
    Optional<ErrorType> errorType = lookupErrorType(errorTypeIdentifier);
    if (!errorType.isPresent()) {
      errorType = ofNullable(this.internalErrorTypes.get(errorTypeIdentifier));
    }
    return errorType;
  }

  /**
   * Gets the {@link ErrorType} instance for ANY error type.
   *
   * @return the ANY error type
   */
  public ErrorType getAnyErrorType() {
    return ANY_ERROR_TYPE;
  }

  /**
   * Gets the {@link ErrorType} instance for SOURCE error type.
   *
   * @return the SOURCE error type
   */
  public ErrorType getSourceErrorType() {
    return SOURCE_ERROR_TYPE;
  }

  /**
   * Gets the {@link ErrorType} instance for SOURCE_RESPONSE error type.
   *
   * @return the SOURCE_RESPONSE error type
   */
  public ErrorType getSourceResponseErrorType() {
    return SOURCE_RESPONSE_ERROR_TYPE;
  }

  /**
   * Gets the {@link ErrorType} instance for CRITICAL error type.
   *
   * @return the CRITICAL error type
   */
  public ErrorType getCriticalErrorType() {
    return CRITICAL_ERROR_TYPE;
  }
}
