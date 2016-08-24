/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static org.mule.runtime.core.config.ExceptionHelper.getRootMuleException;
import static org.mule.runtime.core.message.ErrorTypeBuilder.GENERAL;
import static org.mule.runtime.core.util.Preconditions.checkState;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleException;

/**
 *
 * @since 4.0
 */
public final class ErrorBuilder {

  private Throwable exception;
  private String description;
  private String detailedDescription;
  private ErrorType errorType;
  private MuleMessage errorMessage;

  /**
   * Constructor to create a new builder from scratch.
   */
  public ErrorBuilder() {}

  /**
   * Constructor to create a new builder using the information of an exception
   * for default error parametrization.
   *
   * @param e the exception to use from which the error will be created.
   */
  public ErrorBuilder(Throwable e) {
    this.exception = e;
    String exceptionDescription = e.getMessage() != null ? e.getMessage() : "unknown description";
    this.description = exceptionDescription;
    this.detailedDescription = exceptionDescription;
    MuleException muleRoot = getRootMuleException(exception);
    if (muleRoot != null && muleRoot.getMessage() != null) {
      this.description = muleRoot.getMessage();
    }
    this.errorType = GENERAL;
  }

  /**
   * Sets the exception that causes the error. @see {@link Error#getException()}
   *
   * @param exception the exception that caused the error
   * @return {@code this} builder
     */
  public ErrorBuilder setException(Exception exception) {
    this.exception = exception;
    return this;
  }

  /**
   * Sets the description
   *
   * @param description
   * @return
     */
  public ErrorBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public ErrorBuilder setDetailedDescription(String detailedDescription) {
    this.detailedDescription = detailedDescription;
    return this;
  }

  public ErrorBuilder setErrorType(ErrorType errorType) {
    this.errorType = errorType;
    return this;
  }

  public ErrorBuilder setErrorMessage(MuleMessage errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  public Error build() {
    checkState(exception != null, "error exception cannot be null");
    checkState(description != null, "description exception cannot be null");
    checkState(detailedDescription != null, "detailed description exception cannot be null");
    checkState(errorType != null, "errorType exception cannot be null");
    return new ErrorImplementation(exception, description, detailedDescription, errorType, errorMessage);
  }

  /**
   * Default and only implementation of {@link Error}.
   */
  public final static class ErrorImplementation implements Error {

    private Throwable exception;
    private String description;
    private String detailedDescription;
    private ErrorType errorType;
    private MuleMessage muleMessage;

    private ErrorImplementation(Throwable exception, String description, String detailedDescription, ErrorType errorType,
                                MuleMessage errorMessage) {
      this.exception = exception;
      this.description = description;
      this.detailedDescription = detailedDescription;
      this.errorType = errorType;
      this.muleMessage = errorMessage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
      return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDetailedDescription() {
      return detailedDescription;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ErrorType getErrorType() {
      return errorType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Throwable getException() {
      return exception;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleMessage getErrorMessage() {
      return muleMessage;
    }
  }

}
