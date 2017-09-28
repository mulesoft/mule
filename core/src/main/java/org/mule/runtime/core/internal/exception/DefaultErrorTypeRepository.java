/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.core.api.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.ANY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.UNKNOWN;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Unhandleable.CRITICAL;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_RESPONSE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for the different {@link ErrorType Error Types} in a mule artifact.
 *
 * Only one instance of {@link ErrorType} must exists describing the same combination of error identifier and namespace.
 *
 * @since 4.0
 */
public class DefaultErrorTypeRepository implements ErrorTypeRepository {

  /**
   * Error type that represents all of them that can be handled.
   */
  private static final ErrorType ANY_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(ANY_IDENTIFIER).build();

  /**
   * Parent error type for errors that occur on the source of a flow.
   */
  private static final ErrorType SOURCE_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(SOURCE_ERROR_IDENTIFIER)
          .parentErrorType(ANY_ERROR_TYPE).build();

  /**
   * Parent error type for errors that occur in a source processing a successful response of a flow.
   */
  private static final ErrorType SOURCE_RESPONSE_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(SOURCE_RESPONSE_ERROR_IDENTIFIER)
          .parentErrorType(SOURCE_ERROR_TYPE).build();

  /**
   * Error type for which there's no clear reason for failure. Will be used when no specific match is found.
   */
  private static final ErrorType UNKNOWN_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(UNKNOWN_ERROR_IDENTIFIER)
          .parentErrorType(ANY_ERROR_TYPE).build();

  /**
   * Error type for which there will be no handling since it represents an error so critical it should not be handled. If such an
   * error occurs it will always be propagated. Same for it's children.
   */
  public static final ErrorType CRITICAL_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(CRITICAL_IDENTIFIER)
          .parentErrorType(null).build();

  private Map<ComponentIdentifier, ErrorType> errorTypes = new HashMap<>();
  private Map<ComponentIdentifier, ErrorType> internalErrorTypes = new HashMap<>();

  public DefaultErrorTypeRepository() {
    this.errorTypes.put(ANY, ANY_ERROR_TYPE);
    this.errorTypes.put(SOURCE_RESPONSE, SOURCE_RESPONSE_ERROR_TYPE);
    this.internalErrorTypes.put(SOURCE, SOURCE_ERROR_TYPE);
    this.internalErrorTypes.put(CRITICAL, CRITICAL_ERROR_TYPE);
    this.internalErrorTypes.put(UNKNOWN, UNKNOWN_ERROR_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType addErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    ErrorType errorType = buildErrorType(errorTypeIdentifier, parentErrorType);
    errorTypes.put(errorTypeIdentifier, errorType);
    return errorType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType addInternalErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    ErrorType errorType = buildErrorType(errorTypeIdentifier, parentErrorType);
    internalErrorTypes.put(errorTypeIdentifier, errorType);
    return errorType;
  }

  private ErrorType buildErrorType(ComponentIdentifier identifier, ErrorType parent) {
    if (errorTypes.containsKey(identifier) || internalErrorTypes.containsKey(identifier)) {
      throw new IllegalStateException(format("An error type with identifier '%s' already exists", identifier));
    }
    return ErrorTypeBuilder.builder()
        .namespace(identifier.getNamespace())
        .identifier(identifier.getName())
        .parentErrorType(parent)
        .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ErrorType> lookupErrorType(ComponentIdentifier errorTypeComponentIdentifier) {
    return ofNullable(this.errorTypes.get(errorTypeComponentIdentifier));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ErrorType> getErrorType(ComponentIdentifier errorTypeIdentifier) {
    Optional<ErrorType> errorType = lookupErrorType(errorTypeIdentifier);
    if (!errorType.isPresent()) {
      errorType = ofNullable(this.internalErrorTypes.get(errorTypeIdentifier));
    }
    return errorType;
  }

  @Override
  public Collection<String> getErrorNamespaces() {
    return concat(this.errorTypes.keySet().stream(), this.internalErrorTypes.keySet().stream())
        .map(id -> id.getNamespace().toUpperCase())
        .collect(toSet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType getAnyErrorType() {
    return ANY_ERROR_TYPE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType getSourceErrorType() {
    return SOURCE_ERROR_TYPE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType getSourceResponseErrorType() {
    return SOURCE_RESPONSE_ERROR_TYPE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType getCriticalErrorType() {
    return CRITICAL_ERROR_TYPE;
  }
}
