/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A {@link Matcher<String>} that normalizes line-breaks on the {@link String} being matched.
 * 
 * For example, in Windows, the line-break character, configured in <code>System.getProperty("line.separator")</code> is "\r\n" ;
 * while on linux-based systems it's "\n".
 */
public class StringNormalizeLineBreaks extends TypeSafeMatcher<String> {

  private final Matcher<String> innerMatcher;

  public StringNormalizeLineBreaks(Matcher<String> innerMatcher) {
    this.innerMatcher = innerMatcher;
  }

  @Override
  protected boolean matchesSafely(String aString) {
    String normalizedString = aString.replace("\r\n", "\n");
    return innerMatcher.matches(normalizedString);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a string with normalized line breaks which ").appendDescriptionOf(innerMatcher);
  }

  @Factory
  public static Matcher<String> normalizeLineBreaks(Matcher<String> innerMatcher) {
    return new StringNormalizeLineBreaks(innerMatcher);
  }

}
