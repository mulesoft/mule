/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import static org.hamcrest.Matchers.is;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher to check the message of an exception.
 *
 * @param <T> the type of {@link Throwable} to match
 */
public class ThrowableMessageMatcher<T extends Throwable> extends TypeSafeMatcher<T> {

  private Matcher<String> matcher;

  public ThrowableMessageMatcher(Matcher<String> matcher) {
    this.matcher = matcher;
  }

  @Override
  protected boolean matchesSafely(T item) {
    return matcher.matches(item.getMessage());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("exception with message ");
    description.appendDescriptionOf(matcher);
  }

  public static <T extends Throwable> Matcher<T> hasMessage(final Matcher<String> matcher) {
    return new ThrowableMessageMatcher<>(matcher);
  }

  public static <T extends Throwable> Matcher<T> hasMessage(final String expectedMessage) {
    return new ThrowableMessageMatcher<>(is(expectedMessage));
  }
}
