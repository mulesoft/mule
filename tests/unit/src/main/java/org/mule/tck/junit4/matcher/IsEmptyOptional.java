/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.junit4.matcher;

import java.util.Optional;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies that an {@link Optional} is empty.
 *
 * @since 4.0
 */
public class IsEmptyOptional<T> extends TypeSafeMatcher<Optional<T>> {

  @Override
  protected boolean matchesSafely(Optional<T> item) {
    return !item.isPresent();
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("an empty Optional");
  }

  @Override
  protected void describeMismatchSafely(Optional<T> item, Description mismatchDescription) {
    mismatchDescription.appendText(String.format("got an Optional with a %s", item.get().getClass().getSimpleName()));
  }

  public static <T> IsEmptyOptional<T> empty() {
    return new IsEmptyOptional<>();
  }
}
