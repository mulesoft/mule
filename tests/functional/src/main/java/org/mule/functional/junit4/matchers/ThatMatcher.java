/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

/**
 * Decorates a Matcher maintaining all its behaviour but allowing matcher nesting to be more expressive, much like {@link Is}.
 *
 * @param <T> the type of matcher it wraps
 */
public class ThatMatcher<T> extends BaseMatcher<T> {

  private Matcher<T> delegateMatcher;

  public ThatMatcher(Matcher<T> matcher) {
    this.delegateMatcher = matcher;
  }

  @Override
  public boolean matches(Object item) {
    return delegateMatcher.matches(item);
  }

  @Override
  public void describeTo(Description description) {
    delegateMatcher.describeTo(description);
  }

  @Override
  public void describeMismatch(Object item, Description mismatchDescription) {
    delegateMatcher.describeMismatch(item, mismatchDescription);
  }

  @Factory
  public static <T> Matcher<T> that(Matcher<T> matcher) {
    return new ThatMatcher<>(matcher);
  }
}
