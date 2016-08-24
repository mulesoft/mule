/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static org.mule.runtime.core.util.Preconditions.checkState;

import org.mule.runtime.api.message.ErrorType;

/**
 * Builder for {@link ErrorType}. This must be the only mechanism to create an instance
 * of {@code ErrorType}.
 *
 * @since 4.0
 */
public final class ErrorTypeBuilder {

  private static final String MULE_NAMESPACE = "mule";
  private static final String ANY_STRING_REPRESENTATION = "ANY";
  private static final String GENERAL_STRING_REPRESENTATION = "GENERAL";

  public static final ErrorType ANY =
      new ErrorTypeBuilder().setNamespace(MULE_NAMESPACE).setStringRepresentation(ANY_STRING_REPRESENTATION).build();
  public static final ErrorType GENERAL = new ErrorTypeBuilder().setNamespace(MULE_NAMESPACE)
      .setStringRepresentation(GENERAL_STRING_REPRESENTATION).setParentErrorType(ANY).build();

  private String stringRepresentation;
  private String namespace;
  private ErrorType parentErrorType;

  /**
   * Sets the error type string representation. @see {@link ErrorType#getStringRepresentation()}
   *
   * @param stringRepresentation the string representation
   * @return {@code this} builder
     */
  public ErrorTypeBuilder setStringRepresentation(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
    return this;
  }

  /**
   * Sets the error type namespace. @see {@link ErrorType#getNamespace()}
   *
   * @param namespace the error type namespace
   * @return {@code this} builder
     */
  public ErrorTypeBuilder setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Sets the parent error type. @see {@link ErrorType#getParentErrorType()}
   *
   * @param parentErrorType the parent error type
   * @return {@code this} builder
     */
  public ErrorTypeBuilder setParentErrorType(ErrorType parentErrorType) {
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
    if (!stringRepresentation.equals(ANY_STRING_REPRESENTATION)) {
      checkState(parentErrorType != null, "parent error type cannot be null");
    }
    return new ErrorTypeImplementation(stringRepresentation, namespace, parentErrorType);
  }

  /**
   * Default and only implementation of {@link ErrorType}
   */
  public final static class ErrorTypeImplementation implements ErrorType {

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
