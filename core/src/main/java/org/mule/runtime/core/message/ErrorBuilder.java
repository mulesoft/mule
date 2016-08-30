/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static org.mule.runtime.core.config.ExceptionHelper.getRootMuleException;
import static org.mule.runtime.core.util.Preconditions.checkState;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleException;

/**
 * Builder for {@link Error} instances.
 *
 * @since 4.0
 */
public final class ErrorBuilder {

  private Throwable exception;
  private String description;
  private String detailedDescription;
  private ErrorType errorType;
  private MuleMessage errorMessage;

  public static ErrorBuilder builder() {
    return new ErrorBuilder();
  }

  public static ErrorBuilder builder(Throwable e) {
    return new ErrorBuilder(e);
  }


  /**
   * Constructor to create a new builder from scratch.
   */
  private ErrorBuilder() {}

  /**
   * Constructor to create a new builder using the information of an exception
   * for default error parametrization.
   *
   * @param e the exception to use from which the error will be created.
   */
  private ErrorBuilder(Throwable e) {
    this.exception = e;
    String exceptionDescription = e.getMessage() != null ? e.getMessage() : "unknown description";
    this.description = exceptionDescription;
    this.detailedDescription = exceptionDescription;
    MuleException muleRoot = getRootMuleException(exception);
    if (muleRoot != null && muleRoot.getMessage() != null) {
      this.description = muleRoot.getMessage();
    }
  }

  /**
   * Sets the exception that causes the error. @see {@link Error#getException()}
   *
   * @param exception the exception that caused the error
   * @return {@code this} builder
     */
  public ErrorBuilder exception(Exception exception) {
    this.exception = exception;
    return this;
  }

  /**
   * Sets the description of the error.
   *
   * The description if meant to be a short text that describes the error and should not contain any
   * java specific detail.
   *
   * @param description the description
   * @return {@code this} builder
     */
  public ErrorBuilder description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Sets a detailed description of the error.
   *
   * This description should provide as much information as possible for recognize what the problem can be
   * and, if possible, provide information on how to fix it.
   *
   * @param detailedDescription the detailed description
   * @return {@code this} builder
     */
  public ErrorBuilder detailedDescription(String detailedDescription) {
    this.detailedDescription = detailedDescription;
    return this;
  }

  /**
   * Sets the error type of this error. @see {@link ErrorType}.
   *
   * @param errorType the error type
   * @return {@code this} builder
     */
  public ErrorBuilder errorType(ErrorType errorType) {
    this.errorType = errorType;
    return this;
  }

  /**
   * Sets the error message for the error.
   *
   * An error message is a {@link MuleMessage} with information related to the error.
   * For instance, a response from an http:request operation may return a 4XX status code.
   * The content for the whole response can be set in the error message so the information
   * is available during the error handler execution.
   *
   * @param errorMessage
   * @return
     */
  public ErrorBuilder errorMessage(MuleMessage errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Creates the instance of {@code Error} with the supplied configuration.
   *
   * All builder parameters are required except for the error message which may be null.
   *
   * @return the error instance
   */
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
  private final static class ErrorImplementation implements Error {

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
