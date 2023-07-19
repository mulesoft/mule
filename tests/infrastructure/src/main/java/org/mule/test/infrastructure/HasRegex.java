/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.infrastructure;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


public class HasRegex extends TypeSafeMatcher<String> {

  private final String regex;

  private HasRegex(String regex) {
    this.regex = regex;
  }

  public static Matcher<String> hasRegex(String regex) {
    return new HasRegex(regex);
  }


  @Override
  public boolean matchesSafely(String s) {
    return s.matches(regex);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("matches the regex: /" + regex + "/");
  }
}
