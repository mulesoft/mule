/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static org.mule.runtime.api.exception.ExceptionHelper.getRootMuleException;
import static org.mule.runtime.api.exception.ExceptionHelper.containsSuppressedException;
import static org.mule.runtime.api.exception.ExceptionHelper.unwrapSuppressedException;
import static org.mule.runtime.api.util.Preconditions.checkState;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.message.PrivilegedError;

import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link Error} instances.
 *
 * @since 4.0
 */
public final class ErrorBuilder {

  private Throwable exception;
  private String description;
  private String detailedDescription;
  private Component failingComponent;
  private ErrorType errorType;
  private Message errorMessage;
  private List<Error> errors = emptyList();
  private List<Error> suppressedErrors = emptyList();

  public static ErrorBuilder builder() {
    return new ErrorBuilder();
  }

  public static ErrorBuilder builder(Throwable e) {
    return new ErrorBuilder(e);
  }

  public static ErrorBuilder builder(Error e) {
    return new ErrorBuilder(e);
  }

  /**
   * Constructor to create a new builder from scratch.
   */
  private ErrorBuilder() {}

  /**
   * Constructor to create a new builder using the information of an exception for default error parametrization.
   *
   * @param e the exception to use from which the error will be created.
   */
  private ErrorBuilder(Throwable e) {
    Throwable cause = e;
    exception = cause;
    String exceptionDescription = e.getMessage() != null ? e.getMessage() : "unknown description";
    this.description = exceptionDescription;
    this.detailedDescription = exceptionDescription;
    if (containsSuppressedException(cause)) {
      // We need to unwrap MuleExceptions that were suppressed since they are not meant to be returned as error causes
      cause = unwrapSuppressedException(cause);
      exception = cause;
      addSuppressedErrors((MuleException) e);
      updateErrorDescription((MuleException) e);
    } else {
      MuleException muleRoot = getRootMuleException(cause);
      if (muleRoot != null) {
        addSuppressedErrors(muleRoot);
        updateErrorDescription(muleRoot);
      }
    }
    if (cause instanceof ErrorMessageAwareException) {
      this.errorMessage = ((ErrorMessageAwareException) cause).getErrorMessage();
    }
    if (cause instanceof ComposedErrorException) {
      this.errors = ((ComposedErrorException) cause).getErrors();
    }
  }

  /**
   * Given a {@link MuleException}, sets the <code>description</code> field with the message of it's root cause, taking into
   * account possible suppressions.
   *
   * @param muleException (must be the result of a
   *                      {@link org.mule.runtime.api.exception.ExceptionHelper#getRootMuleException(Throwable)} call.
   */
  private void updateErrorDescription(MuleException muleException) {
    MuleException muleRoot = muleException;
    // Finding the first suppression root message (if present) is needed to make the suppressions backward compatible
    List<MuleException> suppressedCauses = muleException.getExceptionInfo().getSuppressedCauses();
    if (!suppressedCauses.isEmpty()) {
      muleRoot = getRootMuleException(suppressedCauses.get(suppressedCauses.size() - 1));
    }
    if (muleRoot.getMessage() != null) {
      description = muleRoot.getMessage();
    }
  }

  /**
   * Assigns to the <code>suppressedErrors</code> field all the {@link Error} instances that the given {@link MuleException}
   * inform as suppressed.
   *
   * @param muleException Given {@link MuleException}.
   * @see SuppressedMuleException
   */
  private void addSuppressedErrors(MuleException muleException) {
    List<MuleException> suppressedCauses = muleException.getExceptionInfo().getSuppressedCauses();
    if (!suppressedCauses.isEmpty()) {
      List<Error> suppressions = new ArrayList<>(suppressedCauses.size());
      for (MuleException suppressedException : suppressedCauses) {
        if (suppressedException instanceof MessagingException) {
          ((MessagingException) suppressedException).getEvent().getError().ifPresent(error -> {
            suppressions.add(error);
            // First suppressed error cause needs to be set in order to maintain backwards compatibility
            // with the exception of the RetryPolicyExhaustedException, because
            // UntilSuccessfulRouter was removing the underlying MessagingException until 4.2.2
            if (!(muleException instanceof RetryPolicyExhaustedException)) {
              exception = error.getCause();
            }
          });
        }
      }
      suppressedErrors = suppressions;
    }
  }

  /**
   * Constructor to create a new builder using the information of another error for default error parametrization.
   *
   * @param e the error to base on
   */
  private ErrorBuilder(Error e) {
    this.description = e.getDescription();
    this.detailedDescription = e.getDetailedDescription();
    this.exception = e.getCause();
    this.errorType = e.getErrorType();
    this.errorMessage = e.getErrorMessage();
    this.errors = e.getChildErrors();
    this.suppressedErrors = ((PrivilegedError) e).getSuppressedErrors();
  }

  /**
   * Sets the exception that causes the error. @see {@link Error#getCause()}
   *
   * @param exception the exception that caused the error
   * @return {@code this} builder
   */
  public ErrorBuilder exception(Throwable exception) {
    this.exception = exception;
    return this;
  }

  /**
   * Sets the description of the error.
   *
   * The description if meant to be a short text that describes the error and should not contain any java specific detail.
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
   * This description should provide as much information as possible for recognize what the problem can be and, if possible,
   * provide information on how to fix it.
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
   * Sets the component where this error was generated
   *
   * @param failingComponent
   * @return {@code this} builder
   *
   * @since 4.3
   */
  public ErrorBuilder failingComponent(Component failingComponent) {
    this.failingComponent = failingComponent;
    return this;
  }

  /**
   * Sets the error message for the error.
   *
   * An error message is a {@link Message} with information related to the error. For instance, a response from an http:request
   * operation may return a 4XX status code. The content for the whole response can be set in the error message so the information
   * is available during the error handler execution.
   *
   * @param errorMessage
   * @return {@code this} builder
   */
  public ErrorBuilder errorMessage(Message errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Sets the child errors, if any.
   *
   * This errors represent the failures that caused the error being created. For instance, a scatter gather may aggregate all
   * routes and fail when any do, making the route errors available in this list.
   *
   * @param errors a list of errors that caused this one
   * @return {@code this} builder
   */
  public ErrorBuilder errors(List<Error> errors) {
    this.errors = errors;
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
    checkState(exception != null, "exception cannot be null");
    checkState(description != null, "description cannot be null");
    checkState(detailedDescription != null, "detailedDescription cannot be null");
    checkState(errorType != null, "errorType cannot be null");
    return new DeserializableErrorImplementation(exception, description, detailedDescription, failingComponent, errorType,
                                                 errorMessage, errors, suppressedErrors);
  }

  /**
   * Default and only implementation of {@link Error}.
   *
   * @deprecated - Use DeserializableErrorImplementation instead. It's deprecated because this class implements Serializable but
   *             it contains an attribute of type Component, which isn't Serializable.
   */
  @Deprecated
  // TODO MULE-19411: Remove this implementation with DeserializableErrorImplementation.
  static final class ErrorImplementation implements PrivilegedError {

    private static final long serialVersionUID = -6904692174522094021L;

    private final Throwable exception;
    private final String description;
    private final String detailedDescription;
    private final Component failingComponent;
    private final ErrorType errorType;
    private final Message muleMessage;
    private final List<Error> errors;
    private final List<Error> suppressedErrors;

    private Object readResolve() throws ObjectStreamException {
      return builder(this).build();
    }

    ErrorImplementation(Throwable exception, String description, String detailedDescription,
                        Component failingComponent, ErrorType errorType,
                        Message errorMessage, List<Error> errors, List<Error> suppressedErrors) {
      this.exception = exception;
      this.description = description;
      this.detailedDescription = detailedDescription;
      this.failingComponent = failingComponent;
      this.errorType = errorType;
      this.muleMessage = errorMessage;
      this.errors = unmodifiableList(errors);
      this.suppressedErrors = unmodifiableList(suppressedErrors);
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

    @Override
    public String getFailingComponent() {
      return failingComponent != null ? failingComponent.getRepresentation() : null;
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
    public Throwable getCause() {
      return exception;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message getErrorMessage() {
      return muleMessage;
    }

    @Override
    public List<Error> getChildErrors() {
      return errors;
    }

    @Override
    public List<Error> getSuppressedErrors() {
      return suppressedErrors;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(120);

      // format message for multi-line output, single-line is not readable
      buf.append(lineSeparator());
      buf.append(getClass().getName());
      buf.append(lineSeparator());
      buf.append("{");
      buf.append(lineSeparator());
      buf.append("  description=").append(description);
      buf.append(lineSeparator());
      buf.append("  detailedDescription=").append(detailedDescription);
      buf.append(lineSeparator());
      buf.append("  errorType=").append(errorType);
      buf.append(lineSeparator());
      buf.append("  cause=").append(exception.getClass().getName());
      buf.append(lineSeparator());
      buf.append("  errorMessage=").append(defaultIfNull(muleMessage, "-"));
      buf.append(lineSeparator());
      buf.append("  suppressedErrors=").append(suppressedErrors);
      buf.append(lineSeparator());
      buf.append("  childErrors=").append(errors);
      buf.append(lineSeparator());
      buf.append('}');
      return buf.toString();
    }

  }

  /**
   * Default and only non-deprecated implementation of {@link Error}.
   */
  // This is public so that DataWeave can get and invoke its methods and not fallback to change the accessibility of its fields
  public static final class DeserializableErrorImplementation implements PrivilegedError {

    private static final long serialVersionUID = 6703483143042822990L;

    private final Throwable exception;
    private final String description;
    private final String detailedDescription;
    private final String failingComponent;
    private final ErrorType errorType;
    private final Message muleMessage;
    private final List<Error> errors;
    private final List<Error> suppressedErrors;
    private final String dslSource;

    private DeserializableErrorImplementation(Throwable exception, String description, String detailedDescription,
                                              Component failingComponent, ErrorType errorType,
                                              Message errorMessage, List<Error> errors, List<Error> suppressedErrors) {
      this.exception = exception;
      this.description = description;
      this.detailedDescription = detailedDescription;
      this.failingComponent = failingComponent != null ? failingComponent.getRepresentation() : null;
      this.errorType = errorType;
      this.muleMessage = errorMessage;
      this.errors = unmodifiableList(errors);
      this.suppressedErrors = unmodifiableList(suppressedErrors);
      this.dslSource = failingComponent != null ? failingComponent.getDslSource() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDslSource() {
      return dslSource;
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

    @Override
    public String getFailingComponent() {
      return failingComponent;
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
    public Throwable getCause() {
      return exception;
    }

    public Throwable getException() {
      return exception;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message getErrorMessage() {
      return muleMessage;
    }

    @Override
    public List<Error> getChildErrors() {
      return errors;
    }

    @Override
    public List<Error> getSuppressedErrors() {
      return suppressedErrors;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(120);

      // format message for multi-line output, single-line is not readable
      buf.append(lineSeparator());
      buf.append(getClass().getName());
      buf.append(lineSeparator());
      buf.append("{");
      buf.append(lineSeparator());
      buf.append("  description=").append(description);
      buf.append(lineSeparator());
      buf.append("  detailedDescription=").append(detailedDescription);
      buf.append(lineSeparator());
      buf.append("  errorType=").append(errorType);
      buf.append(lineSeparator());
      buf.append("  cause=").append(exception.getClass().getName());
      buf.append(lineSeparator());
      buf.append("  errorMessage=").append(defaultIfNull(muleMessage, "-"));
      buf.append(lineSeparator());
      buf.append("  suppressedErrors=").append(suppressedErrors);
      buf.append(lineSeparator());
      buf.append("  childErrors=").append(errors);
      buf.append(lineSeparator());
      buf.append('}');
      return buf.toString();
    }
  }
}
