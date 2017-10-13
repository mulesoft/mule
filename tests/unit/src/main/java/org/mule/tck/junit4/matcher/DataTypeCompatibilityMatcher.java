/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import org.mule.runtime.api.metadata.DataType;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class DataTypeCompatibilityMatcher extends TypeSafeMatcher<DataType> {

  private final DataType otherDataType;

  public DataTypeCompatibilityMatcher(DataType otherDataType) {
    this.otherDataType = otherDataType;
  }

  @Override
  protected boolean matchesSafely(DataType dataType) {
    return dataType.isCompatibleWith(otherDataType);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a dataType compatible with ").appendValue(otherDataType);
  }

  @Override
  protected void describeMismatchSafely(DataType dataType, Description mismatchDescription) {
    mismatchDescription.appendText("got ").appendValue(dataType);
  }

  @Factory
  public static Matcher<DataType> compatibleWith(DataType dataType) {
    return new DataTypeCompatibilityMatcher(dataType);
  }
}
