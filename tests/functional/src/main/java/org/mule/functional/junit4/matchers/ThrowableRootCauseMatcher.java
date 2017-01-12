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
 * Matcher to check the root cause of an exception.
 *
 * @param <T> the type of {@link Throwable} to match
 */
public class ThrowableRootCauseMatcher<T extends Throwable> extends TypeSafeMatcher<T> {

  private final Matcher<T> matcher;

  public ThrowableRootCauseMatcher(Matcher<T> matcher) {
    this.matcher = matcher;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("exception with root cause ");
    description.appendDescriptionOf(matcher);
  }

  @Override
  protected boolean matchesSafely(T item) {
    return matcher.matches(getRootCause(item));
  }

  @Override
  protected void describeMismatchSafely(T item, Description description) {
    description.appendText("root cause ");
    matcher.describeMismatch(getRootCause(item), description);
  }

  private static Throwable getRootCause(Throwable t) {
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t;
  }

  @Factory
  public static <T extends Throwable> Matcher<T> hasRootCause(final Matcher<T> matcher) {
    return new ThrowableRootCauseMatcher<>(matcher);
  }

}
