/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.api.message.Message;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies a {@link Message}'s attributes, using a specific {@link Message#getAttributes()} matcher.
 *
 * @since 4.0
 */
public class IsMessageWithAttributes<T> extends TypeSafeMatcher<Message> {

  private final Matcher<T> attributesMatcher;

  public IsMessageWithAttributes(Matcher<T> attributesMatcher) {
    this.attributesMatcher = attributesMatcher;
  }

  @Override
  protected boolean matchesSafely(Message message) {
    return attributesMatcher.matches(message.getAttributes().getValue());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a message with ");
    description.appendDescriptionOf(attributesMatcher);
  }

  @Override
  protected void describeMismatchSafely(Message message, Description mismatchDescription) {
    mismatchDescription.appendText("got a message that ");
    attributesMatcher.describeMismatch(message.getAttributes().getValue(), mismatchDescription);
  }
}
