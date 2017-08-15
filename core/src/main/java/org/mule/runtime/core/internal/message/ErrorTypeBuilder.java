/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static java.lang.String.format;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;

import org.mule.runtime.api.message.ErrorType;

/**
 * Builder for {@link ErrorType}. This must be the only mechanism to create an instance of {@code ErrorType}.
 *
 * @since 4.0
 */
public final class ErrorTypeBuilder {

  private String identifier;
  private String namespace;
  private ErrorType parentErrorType;

  public static ErrorTypeBuilder builder() {
    return new ErrorTypeBuilder();
  }

  private ErrorTypeBuilder() {}

  /**
   * Sets the error type identifier. @see {@link ErrorType#getIdentifier()}.
   *
   * The identifier must be unique within the same namespace.
   *
   * @param identifier the string representation
   * @return {@code this} builder
   */
  public ErrorTypeBuilder identifier(String identifier) {
    this.identifier = identifier;
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
    checkState(identifier != null, "string representation cannot be null");
    checkState(namespace != null, "namespace representation cannot be null");
    if (!isOrphan()) {
      checkState(parentErrorType != null, "parent error type cannot be null");
    }
    return new ErrorTypeImplementation(identifier, namespace, parentErrorType);
  }

  private boolean isOrphan() {
    return (identifier.equals(ANY_IDENTIFIER) || identifier.equals(CRITICAL_IDENTIFIER)) && namespace.equals(CORE_NAMESPACE_NAME);
  }

  /**
   * Default and only implementation of {@link ErrorType}
   */
  private final static class ErrorTypeImplementation implements ErrorType {

    private String identifier;
    private String namespace;
    private ErrorType parentErrorType;

    private ErrorTypeImplementation(String identifier, String namespace, ErrorType parentErrorType) {
      this.identifier = identifier;
      this.namespace = namespace;
      this.parentErrorType = parentErrorType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier() {
      return identifier;
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

    @Override
    public String toString() {
      return format("%s:%s", namespace, identifier);
    }

    @Override
    public boolean equals(Object obj) {
      return reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
      return reflectionHashCode(this);
    }
  }

}
