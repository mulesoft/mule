/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher.ast;

import org.mule.runtime.ast.api.validation.ValidationResultItem;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ValidationResultItemMessageMatcher extends TypeSafeMatcher<ValidationResultItem> {

  public static final Matcher<ValidationResultItem> message(Matcher<String> messageMatcher) {
    return new ValidationResultItemMessageMatcher(messageMatcher);
  }

  private final Matcher<String> messageMatcher;

  private ValidationResultItemMessageMatcher(Matcher<String> messageMatcher) {
    this.messageMatcher = messageMatcher;
  }

  @Override
  protected boolean matchesSafely(ValidationResultItem item) {
    return messageMatcher.matches(item.getMessage());
  }

  @Override
  public void describeTo(Description description) {
    messageMatcher.describeTo(description.appendText("a ValidationResultItem with "));
  }

  protected void describeMismatchSafely(ValidationResultItem item, Description description) {
    description.appendText("was a ValidationResultItem with message ").appendValue(item.getMessage());
  }

}
