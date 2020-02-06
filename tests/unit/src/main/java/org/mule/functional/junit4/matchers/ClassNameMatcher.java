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
 * Matcher to check the class name of an {@link Object}
 * @param <T> the type of {@link Object} to match
 */
public class ClassNameMatcher<T> extends TypeSafeMatcher<T> {

  private final Matcher<String> matcher;

  private ClassNameMatcher(Matcher<String> matcher) {
    this.matcher = matcher;
  }

  public void describeTo(Description description) {
    description.appendText("object with class name ");
    description.appendDescriptionOf(matcher);
  }

  @Override
  protected boolean matchesSafely(T item) {
    return matcher.matches(item.getClass().getName());
  }

  @Override
  protected void describeMismatchSafely(T item, Description description) {
    description.appendText("class name ");
    matcher.describeMismatch(item.getClass().getName(), description);
  }

  @Factory
  public static <T> Matcher<T> hasClassName(final Matcher<String> matcher) {
    return new ClassNameMatcher<T>(matcher);
  }

}
