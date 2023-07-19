/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.infrastructure.profiling.tracing;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.emptyOrNullString;

import org.mule.runtime.tracer.api.sniffer.CapturedEventData;

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
  private final Matcher<String> exceptionTypeMatcher;
  private Matcher<String> exceptionDescriptionMatcher;
  private final Matcher<String> exceptionEscapedMatcher = equalTo("true");
  private Matcher<String> exceptionStackTraceMatcher = not(emptyOrNullString());

  private ExceptionEventMatcher(String errorType) {
    this.exceptionTypeMatcher = equalTo(errorType);
    this.exceptionDescriptionMatcher = any(String.class);
  }

  public static ExceptionEventMatcher withType(String type) {
    return new ExceptionEventMatcher(type);
  }

  public ExceptionEventMatcher withStackTrace(String stackTrace) {
    this.exceptionStackTraceMatcher = equalTo(stackTrace);
    return this;
  }

  public ExceptionEventMatcher withDescription(String description) {
    this.exceptionDescriptionMatcher = equalTo(description);
    return this;
  }

  @Override
  protected boolean matchesSafely(CapturedEventData event) {
    return eventNameMatcher.matches(event.getName())
        && exceptionDescriptionMatcher.matches(event.getAttributes().get(OTEL_EXCEPTION_MESSAGE_KEY))
        && exceptionEscapedMatcher.matches(event.getAttributes().get(OTEL_EXCEPTION_ESCAPED_KEY))
        && exceptionStackTraceMatcher.matches(event.getAttributes().get(OTEL_EXCEPTION_STACK_TRACE_KEY))
        && exceptionTypeMatcher.matches(event.getAttributes().get(OTEL_EXCEPTION_TYPE_KEY));
  }

  @Override
  public void describeTo(Description description) {
    description
        .appendText("event: {name: \"exception\", type: ").appendDescriptionOf(exceptionTypeMatcher)
        .appendText(", description: ").appendDescriptionOf(exceptionDescriptionMatcher)
        .appendText(", stacktrace: ").appendDescriptionOf(exceptionStackTraceMatcher)
        .appendText(", escaped: ").appendDescriptionOf(exceptionEscapedMatcher)
        .appendText("}");
  }
}
