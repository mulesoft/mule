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
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * {@link Matcher} for {@link BaseEvent} instances.
 *
 * @since 4.0
 */
public class EventMatcher extends TypeSafeMatcher<BaseEvent> {

  private Matcher<Message> messageMatcher;
  private Matcher<ErrorType> errorTypeMatcher;

  public EventMatcher(Matcher<Message> messageMatcher, Matcher<ErrorType> errorTypeMatcher) {
    this.errorTypeMatcher = errorTypeMatcher;
  }

  public static EventMatcher hasErrorTypeThat(Matcher<ErrorType> errorTypeMatcher) {
    return new EventMatcher(null, errorTypeMatcher);
  }

  public static EventMatcher hasErrorType(ErrorTypeDefinition type) {
    return new EventMatcher(null, errorType(type));
  }

  public static EventMatcher hasErrorType(String namespace, String type) {
    return new EventMatcher(null, errorType(namespace, type));
  }

  public static EventMatcher hasErrorType(Matcher<String> namespace, Matcher<String> type) {
    return new EventMatcher(null, errorType(namespace, type));
  }

  public static EventMatcher hasMessage(Matcher<Message> messageMatcher) {
    return new EventMatcher(messageMatcher, null);
  }

  @Override
  protected boolean matchesSafely(BaseEvent item) {
    return messageMatcher != null ? messageMatcher.matches(item.getMessage()) : true
        && errorTypeMatcher != null ? errorTypeMatcher.matches(item.getError().get().getErrorType()) : true;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("an Event with ");
    if (messageMatcher != null) {
      messageMatcher.describeTo(description);
    }
    if (errorTypeMatcher != null) {
      errorTypeMatcher.describeTo(description);
    }
  }

}
