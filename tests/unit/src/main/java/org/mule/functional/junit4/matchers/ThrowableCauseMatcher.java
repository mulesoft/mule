/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.junit4.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher to check the cause of an exception.
 *
 * @param <T> the type of {@link Throwable} to match
 */
public class ThrowableCauseMatcher<T extends Throwable> extends TypeSafeMatcher<T> {

  private final Matcher<T> matcher;

  public ThrowableCauseMatcher(Matcher<T> matcher) {
    this.matcher = matcher;
  }

  public void describeTo(Description description) {
    description.appendText("exception with cause ");
    description.appendDescriptionOf(matcher);
  }

  @Override
  protected boolean matchesSafely(T item) {
    return matcher.matches(item.getCause());
  }

  @Override
  protected void describeMismatchSafely(T item, Description description) {
    description.appendText("cause ");
    matcher.describeMismatch(item.getCause(), description);
  }

  public static <T extends Throwable> Matcher<T> hasCause(final Matcher<T> matcher) {
    return new ThrowableCauseMatcher<T>(matcher);
  }

}
