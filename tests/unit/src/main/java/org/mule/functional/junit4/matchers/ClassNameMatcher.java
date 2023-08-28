/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.junit4.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher to check the class name of an {@link Object}
 * 
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

  public static <T> Matcher<T> hasClassName(final Matcher<String> matcher) {
    return new ClassNameMatcher<T>(matcher);
  }

}
