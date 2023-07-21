/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.data.sample;

import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Utilities for testing {@link SampleDataProvider} functionalities
 *
 * @since 4.4.0
 */
public final class SampleDataTestUtils {

  private SampleDataTestUtils() {}

  /**
   * Creates a matcher that tests that a {@link SampleDataException} has the given {@code failureCode}
   *
   * @param failureCode the expected failure code
   * @return a {@link Matcher}
   */
  public static Matcher<SampleDataException> exceptionMatcher(String failureCode) {
    return new BaseMatcher<SampleDataException>() {

      @Override
      public boolean matches(Object o) {
        return ((SampleDataException) o).getFailureCode().equals(failureCode);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Unexpected exception code");
      }
    };
  }
}
