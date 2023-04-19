/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.profiling.tracing;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.emptyOrNullString;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.tracer.api.sniffer.CapturedEventData;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Hamcrest matcher that can match against {@link CapturedEventData} instances.
 */
public class ExceptionEventMatcher extends TypeSafeMatcher<CapturedEventData> {

  public static final String OTEL_EXCEPTION_EVENT_NAME = "exception";
  public static final String OTEL_EXCEPTION_TYPE_KEY = "exception.type";
  public static final String OTEL_EXCEPTION_MESSAGE_KEY = "exception.message";
  public static final String OTEL_EXCEPTION_STACK_TRACE_KEY = "exception.stacktrace";
  public static final String OTEL_EXCEPTION_ESCAPED_KEY = "exception.escaped";

  private final Matcher<String> eventNameMatcher = equalTo(OTEL_EXCEPTION_EVENT_NAME);
  private final ErrorTypeMatcher errorTypeMatcher;
  private final Matcher<String> errorDescriptionMatcher;
  private final Matcher<String> errorEscapedMatcher = equalTo("true");
  private final Matcher<String> errorStackTraceMatcher = not(emptyOrNullString());

  public ExceptionEventMatcher(String errorType, String errorDescription) {
    String[] errorTypeComponents = errorType.split(":");
    if (errorTypeComponents.length != 2) {
      throw new IllegalArgumentException(format("Wrong error type: %s", errorType));
    }
    this.errorTypeMatcher = ErrorTypeMatcher.errorType(errorTypeComponents[0], errorTypeComponents[1]);
    this.errorDescriptionMatcher = equalTo(errorDescription);
  }

  public ExceptionEventMatcher(String errorType) {
    String[] errorTypeComponents = errorType.split(":");
    if (errorTypeComponents.length != 2) {
      throw new IllegalArgumentException(format("Wrong error type: %s", errorType));
    }
    this.errorTypeMatcher = ErrorTypeMatcher.errorType(errorTypeComponents[0], errorTypeComponents[1]);
    this.errorDescriptionMatcher = any(String.class);
  }

  @Override
  protected boolean matchesSafely(CapturedEventData event) {
    return eventNameMatcher.matches(event.getName())
        && errorDescriptionMatcher.matches(event.getAttributes().get(OTEL_EXCEPTION_MESSAGE_KEY))
        && errorEscapedMatcher.matches(event.getAttributes().get(OTEL_EXCEPTION_ESCAPED_KEY))
        && errorStackTraceMatcher.matches(event.getAttributes().get(OTEL_EXCEPTION_STACK_TRACE_KEY))
        && errorTypeMatcher.matches(new ErrorType() {

          @Override
          public String getIdentifier() {
            return event.getAttributes().get(OTEL_EXCEPTION_TYPE_KEY) != null
                ? event.getAttributes().get(OTEL_EXCEPTION_TYPE_KEY).toString().split(":")[1]
                : "";
          }

          @Override
          public String getNamespace() {
            return event.getAttributes().get(OTEL_EXCEPTION_TYPE_KEY) != null
                ? event.getAttributes().get(OTEL_EXCEPTION_TYPE_KEY).toString().split(":")[0]
                : "";
          }

          @Override
          public ErrorType getParentErrorType() {
            return null;
          }
        });
  }

  @Override
  public void describeTo(Description description) {

  }
}
