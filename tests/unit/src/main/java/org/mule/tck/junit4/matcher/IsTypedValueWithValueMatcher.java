/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import org.mule.runtime.api.metadata.TypedValue;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies that a {@link TypedValue} has the expected value.
 *
 * @since 4.8
 */
public class IsTypedValueWithValueMatcher<T> extends TypeSafeMatcher<TypedValue<? super T>> {

  private final Matcher<? super T> expectedValueMatcher;

  private IsTypedValueWithValueMatcher(Matcher<? super T> expectedValueMatcher) {
    this.expectedValueMatcher = expectedValueMatcher;
  }

  @Override
  protected boolean matchesSafely(TypedValue<? super T> item) {
    return expectedValueMatcher.matches(item.getValue());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a TypedValue with a value ");
    description.appendDescriptionOf(expectedValueMatcher);
  }

  @Override
  protected void describeMismatchSafely(TypedValue<? super T> item, Description mismatchDescription) {
    mismatchDescription.appendText("got a TypedValue with a value that ");
    expectedValueMatcher.describeMismatch(item.getValue(), mismatchDescription);
  }

  public static <T> IsTypedValueWithValueMatcher<T> aTypedValueWithValue(Matcher<? super T> valueMatcher) {
    return new IsTypedValueWithValueMatcher<>(valueMatcher);
  }
}
