/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.junit4.matcher;

import org.mule.runtime.api.metadata.DataType;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class DataTypeCompatibilityMatcher extends TypeSafeMatcher<DataType> {

  private final DataType dataType;

  public DataTypeCompatibilityMatcher(DataType dataType) {
    this.dataType = dataType;
  }

  @Override
  protected boolean matchesSafely(DataType otherDataType) {
    return dataType.isCompatibleWith(otherDataType);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a dataType assignable to ").appendValue(dataType);
  }

  @Override
  protected void describeMismatchSafely(DataType dataType, Description mismatchDescription) {
    mismatchDescription.appendText("got ").appendValue(dataType);
  }

  public static Matcher<DataType> assignableTo(DataType dataType) {
    return new DataTypeCompatibilityMatcher(dataType);
  }
}
