/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A matcher that will evaluate against the result of a given {@link Function}.
 * 
 * @param <T> the type of the object that provides the object to be matched.
 * @param <R> the type of the object to match against.
 */
public final class FunctionExpressionMatcher<T, R> extends TypeSafeMatcher<T> {

  private Function<T, R> resolver;
  private Matcher<? extends R> matcher;

  private FunctionExpressionMatcher(Function<T, R> resolver, Matcher<? extends R> matcher) {
    this.resolver = resolver;
    this.matcher = matcher;
  }

  /**
   * Builds a matcher that lazily evaluates against the object returned by the given resolver {@link Function}.
   * 
   * @param resolver the function to use to get the object to run the matcher against.
   * @param matcher the matcher to run against the resolved object.
   * @return a matcher that lazily evaluates the object to match.
   */
  @Factory
  public static final <T, R> FunctionExpressionMatcher<T, R> expressionMatches(Function<T, R> resolver,
                                                                               Matcher<? extends R> matcher) {
    return new FunctionExpressionMatcher<>(resolver, matcher);

  }

  @Override
  protected boolean matchesSafely(T item) {
    return matcher.matches(resolver.apply(item));
  }

  @Override
  public void describeTo(Description description) {
    matcher.describeTo(description);
  }
}
