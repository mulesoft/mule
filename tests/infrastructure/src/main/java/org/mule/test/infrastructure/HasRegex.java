/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
