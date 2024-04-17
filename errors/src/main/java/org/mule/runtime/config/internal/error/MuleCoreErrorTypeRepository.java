/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.error;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;

import static org.mule.runtime.ast.api.error.ErrorTypeBuilder.builder;
import static org.mule.runtime.core.api.error.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.ANY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.CLIENT_SECURITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.COMPOSITE_ROUTING;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.CONNECTIVITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.DUPLICATE_MESSAGE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.EXPRESSION;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.NOT_PERMITTED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.RETRY_EXHAUSTED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.ROUTING;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SECURITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SERVER_SECURITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.STREAM_MAXIMUM_SIZE_EXCEEDED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.TIMEOUT;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.TRANSACTION;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.TRANSFORMATION;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.UNKNOWN;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.VALIDATION;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.CRITICAL;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.FATAL;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.FLOW_BACK_PRESSURE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.OVERLOAD;
import static org.mule.runtime.core.api.error.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.SOURCE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.SOURCE_RESPONSE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.ast.api.error.ErrorTypeBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MuleCoreErrorTypeRepository implements ErrorTypeRepository {

  /**
   * Error type that represents all of them that can be handled.
   */
  private static final ErrorType ANY_ERROR_TYPE =
      builder().namespace(CORE_NAMESPACE_NAME).identifier(ANY_IDENTIFIER).build();

  /**
   * Parent error type for errors that occur on the source of a flow.
   */
  private static final ErrorType SOURCE_ERROR_TYPE =
      builder().namespace(CORE_NAMESPACE_NAME).identifier(SOURCE_ERROR_IDENTIFIER)
          .parentErrorType(ANY_ERROR_TYPE).build();

  /**
   * Parent error type for errors that occur in a source processing a successful response of a flow.
   */
  private static final ErrorType SOURCE_RESPONSE_ERROR_TYPE =
      builder().namespace(CORE_NAMESPACE_NAME).identifier(SOURCE_RESPONSE_ERROR_IDENTIFIER)
          .parentErrorType(SOURCE_ERROR_TYPE).build();

  /**
   * Error type for which there's no clear reason for failure. Will be used when no specific match is found.
   */
  private static final ErrorType UNKNOWN_ERROR_TYPE =
      builder().namespace(CORE_NAMESPACE_NAME).identifier(UNKNOWN_ERROR_IDENTIFIER)
          .parentErrorType(ANY_ERROR_TYPE).build();

  /**
   * Error type for which there will be no handling since it represents an error so critical it should not be handled. If such an
   * error occurs it will always be propagated. Same for it's children.
   */
  public static final ErrorType CRITICAL_ERROR_TYPE =
      builder().namespace(CORE_NAMESPACE_NAME).identifier(CRITICAL_IDENTIFIER)
          .parentErrorType(null).build();

  public static final ErrorTypeRepository MULE_CORE_ERROR_TYPE_REPOSITORY = new MuleCoreErrorTypeRepository();

  private final Map<ComponentIdentifier, ErrorType> errorTypes = new HashMap<>();
  private final Map<ComponentIdentifier, ErrorType> internalErrorTypes = new HashMap<>();

  private MuleCoreErrorTypeRepository() {
    this.errorTypes.put(ANY, ANY_ERROR_TYPE);
    this.errorTypes.put(SOURCE_RESPONSE, SOURCE_RESPONSE_ERROR_TYPE);
    this.internalErrorTypes.put(SOURCE, SOURCE_ERROR_TYPE);
    this.internalErrorTypes.put(CRITICAL, CRITICAL_ERROR_TYPE);
    this.internalErrorTypes.put(UNKNOWN, UNKNOWN_ERROR_TYPE);

    doAddErrorType(TRANSFORMATION, getAnyErrorType());
    doAddErrorType(EXPRESSION, getAnyErrorType());
    final ErrorType validationErrorType = doAddErrorType(VALIDATION, getAnyErrorType());
    doAddErrorType(DUPLICATE_MESSAGE, validationErrorType);
    doAddErrorType(REDELIVERY_EXHAUSTED, getAnyErrorType());
    final ErrorType connectivityErrorType = doAddErrorType(CONNECTIVITY, getAnyErrorType());
    doAddErrorType(RETRY_EXHAUSTED, connectivityErrorType);
    doAddErrorType(ROUTING, getAnyErrorType());
    doAddErrorType(SECURITY, getAnyErrorType());
    doAddErrorType(CLIENT_SECURITY, getErrorType(SECURITY).get());
    doAddErrorType(SERVER_SECURITY, getErrorType(SECURITY).get());
    doAddErrorType(NOT_PERMITTED, getErrorType(SERVER_SECURITY).get());
    doAddErrorType(STREAM_MAXIMUM_SIZE_EXCEEDED, getAnyErrorType());
    doAddErrorType(TRANSACTION, getAnyErrorType());

    doAddInternalErrorType(OVERLOAD, getCriticalErrorType());
    doAddInternalErrorType(FLOW_BACK_PRESSURE, getErrorType(OVERLOAD).get());
    doAddInternalErrorType(FATAL, getCriticalErrorType());
    doAddErrorType(TIMEOUT, getAnyErrorType());
    doAddErrorType(COMPOSITE_ROUTING, getErrorType(ROUTING).get());

    final ErrorType sourceErrorType = getSourceErrorType();
    doAddErrorType(SOURCE_RESPONSE_GENERATE, getSourceResponseErrorType());
    doAddErrorType(SOURCE_RESPONSE_SEND, getSourceResponseErrorType());
    doAddInternalErrorType(SOURCE_ERROR_RESPONSE_GENERATE, sourceErrorType);
    doAddInternalErrorType(SOURCE_ERROR_RESPONSE_SEND, sourceErrorType);
  }

  @Override
  public ErrorType addErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ErrorType addInternalErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    throw new UnsupportedOperationException();
  }

  private ErrorType doAddErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    ErrorType errorType = buildErrorType(errorTypeIdentifier, parentErrorType);
    errorTypes.put(errorTypeIdentifier, errorType);
    return errorType;
  }

  private ErrorType doAddInternalErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    ErrorType errorType = buildErrorType(errorTypeIdentifier, parentErrorType);
    internalErrorTypes.put(errorTypeIdentifier, errorType);
    return errorType;
  }

  private ErrorType buildErrorType(ComponentIdentifier identifier, ErrorType parent) {
    if (errorTypes.containsKey(identifier) || internalErrorTypes.containsKey(identifier)) {
      throw new IllegalStateException(format("An error type with identifier '%s' already exists", identifier));
    }
    return builder()
        .namespace(identifier.getNamespace())
        .identifier(identifier.getName())
        .parentErrorType(parent)
        .build();
  }

  @Override
  public Optional<ErrorType> lookupErrorType(ComponentIdentifier errorTypeComponentIdentifier) {
    return ofNullable(this.errorTypes.get(errorTypeComponentIdentifier));
  }

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
    return singleton(CORE_NAMESPACE_NAME);
  }

  @Override
  public ErrorType getAnyErrorType() {
    return ANY_ERROR_TYPE;
  }

  @Override
  public ErrorType getSourceErrorType() {
    return SOURCE_ERROR_TYPE;
  }

  @Override
  public ErrorType getSourceResponseErrorType() {
    return SOURCE_RESPONSE_ERROR_TYPE;
  }

  @Override
  public ErrorType getCriticalErrorType() {
    return CRITICAL_ERROR_TYPE;
  }

  @Override
  public Set<ErrorType> getErrorTypes() {
    return new HashSet<>(errorTypes.values());
  }

  @Override
  public Set<ErrorType> getInternalErrorTypes() {
    return new HashSet<>(internalErrorTypes.values());
  }
}
