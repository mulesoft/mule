/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher.value;

import static java.lang.System.lineSeparator;

import org.mule.runtime.api.value.ValueResult;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies that a {@link ValueResult} is successful.
 *
 * @since 4.3
 */
public class ValueResultSuccessMatcher extends TypeSafeMatcher<ValueResult> {

  private ValueResult item;

  @Override
  public void describeTo(Description description) {
    item.getFailure().ifPresent(failure -> {
      description.appendText("ValueResultFailure (" + failure.getFailureCode() + ":" + lineSeparator());
      description.appendText("\tMessaage: " + failure.getMessage() + lineSeparator());
      description.appendText("\t  Reason: " + failure.getReason() + lineSeparator());
    });
  }

  @Override
  protected boolean matchesSafely(ValueResult item) {
    this.item = item;
    return item.isSuccess() && !item.getFailure().isPresent();
  }

  public static ValueResultSuccessMatcher isSuccess() {
    return new ValueResultSuccessMatcher();
  }

}
