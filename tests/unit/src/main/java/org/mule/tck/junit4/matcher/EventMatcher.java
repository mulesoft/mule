/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Map;

/**
 * {@link Matcher} for {@link CoreEvent} instances.
 *
 * @since 4.0
 */
public class EventMatcher extends TypeSafeMatcher<CoreEvent> {

  private Matcher<Message> messageMatcher;
  private Matcher<Map<String, TypedValue<?>>> variablesMatcher;
  private Matcher<ErrorType> errorTypeMatcher;
  private Matcher<SecurityContext> securityContextMatcher;

  public EventMatcher(Matcher<Message> messageMatcher, Matcher<Map<String, TypedValue<?>>> variablesMatcher,
                      Matcher<ErrorType> errorTypeMatcher, Matcher<SecurityContext> securityContextMatcher) {
    this.messageMatcher = messageMatcher;
    this.variablesMatcher = variablesMatcher;
    this.errorTypeMatcher = errorTypeMatcher;
    this.securityContextMatcher = securityContextMatcher;
  }

  public static EventMatcher hasErrorTypeThat(Matcher<ErrorType> errorTypeMatcher) {
    return new EventMatcher(null, null, errorTypeMatcher, null);
  }

  public static EventMatcher hasErrorType(ErrorTypeDefinition type) {
    return new EventMatcher(null, null, errorType(type), null);
  }

  public static EventMatcher hasErrorType(String namespace, String type) {
    return new EventMatcher(null, null, errorType(namespace, type), null);
  }

  public static EventMatcher hasErrorType(Matcher<String> namespace, Matcher<String> type) {
    return new EventMatcher(null, null, errorType(namespace, type), null);
  }

  public static EventMatcher hasMessage(Matcher<Message> messageMatcher) {
    return new EventMatcher(messageMatcher, null, null, null);
  }

  public static EventMatcher hasVariables(Matcher<Map<String, TypedValue<?>>> variablesMatcher) {
    return new EventMatcher(null, variablesMatcher, null, null);
  }

  public static EventMatcher hasSecurityContext(Matcher<SecurityContext> securityContextMatcher) {
    return new EventMatcher(null, null, null, securityContextMatcher);
  }

  @Override
  protected boolean matchesSafely(CoreEvent item) {
    return (messageMatcher != null ? messageMatcher.matches(item.getMessage()) : true)
        && (variablesMatcher != null ? variablesMatcher.matches(item.getVariables()) : true)
        && (securityContextMatcher != null ? securityContextMatcher.matches(item.getSecurityContext()) : true)
        && (errorTypeMatcher != null ? errorTypeMatcher.matches(item.getError().get().getErrorType()) : true);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("an Event with ");
    if (messageMatcher != null) {
      messageMatcher.describeTo(description);
    }
    if (variablesMatcher != null) {
      variablesMatcher.describeTo(description);
    }
    if (errorTypeMatcher != null) {
      errorTypeMatcher.describeTo(description);
    }
    if (securityContextMatcher != null) {
      securityContextMatcher.describeTo(description);
    }
  }

  @Override
  protected void describeMismatchSafely(CoreEvent item, Description mismatchDescription) {
    mismatchDescription.appendText("was ");
    if (messageMatcher != null) {
      messageMatcher.describeMismatch(item.getMessage(), mismatchDescription);
    }
    if (variablesMatcher != null) {
      variablesMatcher.describeMismatch(item.getVariables(), mismatchDescription);
    }
    if (errorTypeMatcher != null) {
      errorTypeMatcher.describeMismatch(item.getError().get().getErrorType(), mismatchDescription);
    }
    if (securityContextMatcher != null) {
      securityContextMatcher.describeMismatch(item.getSecurityContext(), mismatchDescription);
    }
  }

}
