/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
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

  @Factory
  public static <T extends Throwable> Matcher<T> hasCause(final Matcher<T> matcher) {
    return new ThrowableCauseMatcher<T>(matcher);
  }

}
