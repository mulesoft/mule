/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.matcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Tests if a {@link String} is equal to another string, regardless of the line breaks,helpful when dealing with cross-platform
 * tests
 *
 * @since 4.0
 */
public class IsEqualIgnoringLineBreaks extends TypeSafeMatcher<String> {

  private final String string;

  public IsEqualIgnoringLineBreaks(String string) {
    if (string == null) {
      throw new IllegalArgumentException("Non-null value required by IsEqualIgnoringLineBreaks()");
    } else {
      this.string = string;
    }
  }

  public boolean matchesSafely(String item) {
    String expected = string.replace("\r\n", "").replace("\n", "");
    String actual = item.replace("\r\n", "").replace("\n", "");
    return expected.equals(actual);
  }

  public void describeMismatchSafely(String item, Description mismatchDescription) {
    mismatchDescription.appendText("was ").appendText(item);
  }

  public void describeTo(Description description) {
    description.appendText("equalToIgnoringLineBreaks(").appendValue(this.string).appendText(")");
  }

  public static Matcher<String> equalToIgnoringLineBreaks(String expectedString) {
    return new IsEqualIgnoringLineBreaks(expectedString);
  }
}
