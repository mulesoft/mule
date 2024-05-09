/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.serialization;

import static java.lang.String.format;

import static org.hamcrest.core.Is.is;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies that a {@link NotSerializableTestObject} has the expected name.
 *
 * @since 4.8
 */
public class IsNotSerializableTestObjectWithNameMatcher extends TypeSafeMatcher<NotSerializableTestObject> {

  private final Matcher<String> expectedNameMatcher;

  private IsNotSerializableTestObjectWithNameMatcher(Matcher<String> expectedValueMatcher) {
    this.expectedNameMatcher = expectedValueMatcher;
  }

  @Override
  protected boolean matchesSafely(NotSerializableTestObject item) {
    return expectedNameMatcher.matches(item.getName());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(format("a %s with name ", NotSerializableTestObject.class.getName()));
    description.appendDescriptionOf(expectedNameMatcher);
  }

  @Override
  protected void describeMismatchSafely(NotSerializableTestObject item, Description mismatchDescription) {
    mismatchDescription.appendText(format("got a %s with a name that ", item.getClass().getName()));
    expectedNameMatcher.describeMismatch(item.getName(), mismatchDescription);
  }

  public static IsNotSerializableTestObjectWithNameMatcher aNotSerializableTestObjectWithName(Matcher<String> nameMatcher) {
    return new IsNotSerializableTestObjectWithNameMatcher(nameMatcher);
  }

  public static IsNotSerializableTestObjectWithNameMatcher aNotSerializableTestObjectWithName(String name) {
    return new IsNotSerializableTestObjectWithNameMatcher(is(name));
  }
}
