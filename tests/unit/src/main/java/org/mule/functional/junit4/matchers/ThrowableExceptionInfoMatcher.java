/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.api.exception.MuleException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Map;

/**
 * Matcher to check the additional {@code info} of a MuleException.
 *
 * @param <T> the type of {@link MuleException} to match
 */
public class ThrowableExceptionInfoMatcher<T extends MuleException> extends TypeSafeMatcher<T> {

  private Matcher<Map<? extends String, ?>> matcher;

  public ThrowableExceptionInfoMatcher(Matcher<Map<? extends String, ?>> matcher) {
    this.matcher = matcher;
  }

  @Override
  protected boolean matchesSafely(T item) {
    return matcher.matches(item.getInfo());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("exception with info ");
    description.appendDescriptionOf(matcher);
  }

  public static <T extends MuleException> Matcher<T> hasInfo(final Matcher<Map<? extends String, ?>> matcher) {
    return new ThrowableExceptionInfoMatcher<>(matcher);
  }
}
