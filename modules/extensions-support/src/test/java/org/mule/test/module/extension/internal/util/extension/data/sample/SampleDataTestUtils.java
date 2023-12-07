/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.internal.util.extension.data.sample;

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
