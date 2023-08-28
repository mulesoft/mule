/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.junit4.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
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

  public static <T> Matcher<T> that(Matcher<T> matcher) {
    return new ThatMatcher<>(matcher);
  }
}
