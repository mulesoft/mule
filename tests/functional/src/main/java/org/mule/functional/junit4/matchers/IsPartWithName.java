/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.api.message.PartAttributes;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies whether a {@link MultiPartPayload}'s part has a given name.
 *
 * @since 4.0
 */
public class IsPartWithName extends TypeSafeMatcher<PartAttributes> {

  private final String expectedName;

  public IsPartWithName(String name) {
    this.expectedName = name;
  }

  @Override
  protected boolean matchesSafely(PartAttributes partAttributes) {
    return expectedName.equals(partAttributes.getName());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("part attributes with name ").appendValue(expectedName);
  }

  @Override
  protected void describeMismatchSafely(PartAttributes partAttributes, Description mismatchDescription) {
    mismatchDescription.appendText("got part attributes with name ").appendValue(partAttributes.getName());
  }
}
