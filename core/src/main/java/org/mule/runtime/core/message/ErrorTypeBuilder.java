/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static org.mule.runtime.core.exception.ErrorTypeRepository.ANY_IDENTIFIER;
import static org.mule.runtime.core.exception.ErrorTypeRepository.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.util.Preconditions.checkState;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.exception.ErrorTypeRepository;

/**
 * Builder for {@link ErrorType}. This must be the only mechanism to create an instance
 * of {@code ErrorType}.
 *
 * @since 4.0
 */
public final class ErrorTypeBuilder {

  private String stringRepresentation;
  private String namespace;
  private ErrorType parentErrorType;

  public static ErrorTypeBuilder builder() {
    return new ErrorTypeBuilder();
  }

  private ErrorTypeBuilder() {}

  /**
   * Sets the error type string representation. @see {@link ErrorType#getStringRepresentation()}
   *
   * @param stringRepresentation the string representation
   * @return {@code this} builder
     */
  public ErrorTypeBuilder stringRepresentation(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
    return this;
  }

  /**
   * Sets the error type namespace. @see {@link ErrorType#getNamespace()}
   *
   * @param namespace the error type namespace
   * @return {@code this} builder
     */
  public ErrorTypeBuilder namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Sets the parent error type. @see {@link ErrorType#getParentErrorType()}
   *
   * @param parentErrorType the parent error type
   * @return {@code this} builder
     */
  public ErrorTypeBuilder parentErrorType(ErrorType parentErrorType) {
    this.parentErrorType = parentErrorType;
    return this;
  }

  /**
   * Creates a new instance of the configured error type.
   *
   * @return the error type with the provided configuration.
     */
  public ErrorType build() {
    checkState(stringRepresentation != null, "string representation cannot be null");
    checkState(namespace != null, "namespace representation cannot be null");
    if (!(stringRepresentation.equals(ANY_IDENTIFIER) && namespace.equals(CORE_NAMESPACE_NAME))) {
      checkState(parentErrorType != null, "parent error type cannot be null");
    }
    return new ErrorTypeImplementation(stringRepresentation, namespace, parentErrorType);
  }

  /**
   * Default and only implementation of {@link ErrorType}
   */
  private final static class ErrorTypeImplementation implements ErrorType {

    private String stringRepresentation;
    private String namespace;
    private ErrorType parentErrorType;

    private ErrorTypeImplementation(String stringRepresentation, String namespace, ErrorType parentErrorType) {
      this.stringRepresentation = stringRepresentation;
      this.namespace = namespace;
      this.parentErrorType = parentErrorType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringRepresentation() {
      return stringRepresentation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespace() {
      return namespace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ErrorType getParentErrorType() {
      return parentErrorType;
    }
  }

}
