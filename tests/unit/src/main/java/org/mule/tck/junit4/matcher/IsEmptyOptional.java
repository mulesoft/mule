/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import java.util.Optional;

import org.hamcrest.Description;
import org.hamcrest.Factory;
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

  @Factory
  public static <T> IsEmptyOptional<T> empty() {
    return new IsEmptyOptional<>();
  }
}
