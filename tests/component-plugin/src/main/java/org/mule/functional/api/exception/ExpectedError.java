/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.exception;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.fail;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.EventProcessingException;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;

import org.hamcrest.Matcher;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

/**
 * JUnit rule for Mule errors
 *
 * @since 4.0
 */
public class ExpectedError implements TestRule {

  private Matcher<String> messageMatcher;
  private ErrorTypeMatcher errorTypeMatcher;
  private Matcher<? extends Throwable> causeMatcher;
  private Matcher<? extends CoreEvent> eventMatcher;
  private Matcher<? extends Component> failingComponentMatcher;
  private Matcher<? extends Message> errorMessageMatcher;
  private List<Matcher> matchers;

  /**
   * @return a Rule that expects no error to be thrown (identical to behavior without this Rule)
   */
  public static ExpectedError none() {
    return new ExpectedError();
  }

  private ExpectedError() {
    messageMatcher = null;
    errorTypeMatcher = null;
    causeMatcher = null;
    eventMatcher = null;
    errorMessageMatcher = null;
    matchers = new ArrayList<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Statement apply(Statement statement, Description description) {
    return new ExpectedErrorStatement(statement);
  }

  /**
   * Helper method to configure all the matchers that can be used with this rule at the same time.
   */
  public void expectError(String namespace, String errorTypeDefinition, Class<?> cause, String message) {
    expectErrorType(namespace, errorTypeDefinition)
        .expectCause(instanceOf(cause))
        .expectMessage(containsString(message));
  }

  /**
   * Helper method to configure all the matchers that can be used with this rule at the same time.
   */
  public void expectError(String namespace, ErrorTypeDefinition errorTypeDefinition, Class<?> cause, String message) {
    expectError(namespace, errorTypeDefinition.getType(), cause, message);
  }

  public ExpectedError expectMessage(Matcher<String> matcher) {
    this.messageMatcher = matcher;
    return this;
  }

  public ExpectedError expectErrorType(String namespace, String errorType) {
    this.errorTypeMatcher = errorType(namespace, errorType);
    return this;
  }

  public ExpectedError expectErrorType(Matcher<String> namespaceMatcher, Matcher<String> errorTypeMatcher) {
    this.errorTypeMatcher = errorType(namespaceMatcher, errorTypeMatcher);
    return this;
  }

  public ExpectedError expectErrorType(ErrorTypeMatcher matcher) {
    this.errorTypeMatcher = matcher;
    return this;
  }

  public ExpectedError expectCause(Matcher<? extends Throwable> expectedCause) {
    this.causeMatcher = expectedCause;
    return this;
  }

  public ExpectedError expectEvent(Matcher<? extends CoreEvent> eventMatcher) {
    this.eventMatcher = eventMatcher;
    return this;
  }

  public ExpectedError expectFailingComponent(Matcher<? extends Component> failingComponentMatcher) {
    this.failingComponentMatcher = failingComponentMatcher;
    return this;
  }

  public ExpectedError expectErrorMessage(Matcher<? extends Message> errorEventMatcher) {
    this.errorMessageMatcher = errorEventMatcher;
    return this;
  }

  private boolean expectsThrowable() {
    return errorTypeMatcher != null || causeMatcher != null || eventMatcher != null || failingComponentMatcher != null
        || errorMessageMatcher != null || messageMatcher != null;
  }

  private void failDueToMissingException() {
    fail(format("No exception was thrown during the test \n %s", this.toString()));
  }

  private void failWithNonMatchingException(Exception e) {
    fail(format("An exception was caught but it didn't met the following conditions:\n %s \n Caught exception was:\n %s %s",
                Joiner.on("\n").join(matchers), e, getStackTraceAsString(e)));
  }

  private void failDueToUnexpectedException(Throwable e) {
    fail(format("An exception was caught but it wasn't expected for the test \n Caught exception was:\n %s %s", e,
                getStackTraceAsString(e)));
  }

  private void failDueToExceptionWithoutError(Throwable e, CoreEvent event) {
    fail(format("An exception was caught but it didn't contain information about the error \n Event: \n %s \n Caught exception was:\n %s %s",
                event, e, getStackTraceAsString(e)));
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("EventProcessingException matcher with:\n");

    if (errorTypeMatcher != null) {
      builder.append(format("* An error: %s\n", errorTypeMatcher));
    }

    if (causeMatcher != null) {
      builder.append(format("* A cause: %s\n", causeMatcher));
    }

    if (messageMatcher != null) {
      builder.append(format("* A message: %s\n", messageMatcher));
    }

    if (eventMatcher != null) {
      builder.append(format("* An event: %s\n", eventMatcher));
    }

    if (failingComponentMatcher != null) {
      builder.append(format("* A failingComponent: %s\n", failingComponentMatcher));
    }

    if (errorMessageMatcher != null) {
      builder.append(format("* An error Message: %s\n", errorMessageMatcher));
    }

    return builder.toString();
  }

  private class ExpectedErrorStatement extends Statement {

    private final Statement statement;

    public ExpectedErrorStatement(Statement base) {
      statement = base;
    }

    @Override
    public void evaluate() throws Throwable {
      try {
        statement.evaluate();

      } catch (EventProcessingException exception) {
        if (!expectsThrowable()) {
          failDueToUnexpectedException(exception);
        }

        CoreEvent event = exception.getEvent();
        if (!event.getError().isPresent()) {
          failDueToExceptionWithoutError(exception, event);
        }

        Error error = event.getError().get();
        if (errorTypeMatcher != null && !errorTypeMatcher.matches(error.getErrorType())) {
          matchers.add(errorTypeMatcher);
        }

        if (causeMatcher != null && !causeMatcher.matches(error.getCause())) {
          matchers.add(causeMatcher);
        }

        if (messageMatcher != null && !messageMatcher.matches(error.getDescription())) {
          matchers.add(messageMatcher);
        }

        if (failingComponentMatcher != null && !failingComponentMatcher.matches(exception.getFailingComponent())) {
          matchers.add(failingComponentMatcher);
        }

        if (eventMatcher != null && !eventMatcher.matches(event)) {
          matchers.add(eventMatcher);
        }

        if (errorMessageMatcher != null && !errorMessageMatcher.matches(error.getErrorMessage())) {
          matchers.add(errorMessageMatcher);
        }

        if (!matchers.isEmpty()) {
          failWithNonMatchingException(exception);
        }
        return;

      } catch (AssumptionViolatedException exception) {
        throw exception;
      } catch (Throwable throwable) {
        failDueToUnexpectedException(throwable);
      }

      if (expectsThrowable()) {
        failDueToMissingException();
      }
    }

  }
}
