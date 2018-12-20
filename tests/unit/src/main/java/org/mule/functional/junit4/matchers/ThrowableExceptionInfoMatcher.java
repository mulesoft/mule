/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.api.exception.MuleException;

import org.hamcrest.Description;
import org.hamcrest.Factory;
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

  @Factory
  public static <T extends MuleException> Matcher<T> hasInfo(final Matcher<Map<? extends String, ?>> matcher) {
    return new ThrowableExceptionInfoMatcher<>(matcher);
  }
}
