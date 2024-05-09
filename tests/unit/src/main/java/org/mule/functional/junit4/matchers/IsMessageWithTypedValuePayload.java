/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies a {@link Message}'s payload, using a {@link TypedValue} matcher.
 *
 * @since 4.8
 */
public class IsMessageWithTypedValuePayload<T> extends TypeSafeMatcher<Message> {

  private final Matcher<TypedValue<? super T>> matcher;

  /**
   * Verifies the {@link Message}'s payload is a {@link TypedValue} with a value matching the given matcher.
   */
  public static <T> Matcher<Message> hasPayload(Matcher<TypedValue<? super T>> matcher) {
    return new IsMessageWithTypedValuePayload<>(matcher);
  }

  private IsMessageWithTypedValuePayload(Matcher<TypedValue<? super T>> matcher) {
    this.matcher = matcher;
  }

  @Override
  protected boolean matchesSafely(Message message) {
    return matcher.matches(message.getPayload());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("message with payload ");
    description.appendDescriptionOf(matcher);
  }

  @Override
  protected void describeMismatchSafely(Message message, Description mismatchDescription) {
    mismatchDescription.appendText("got a message with a payload that ");
    matcher.describeMismatch(message.getPayload(), mismatchDescription);
  }
}
