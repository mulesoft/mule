/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.junit4.matcher;

import org.hamcrest.Description;
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

  public static Matcher<String> normalizeLineBreaks(Matcher<String> innerMatcher) {
    return new StringNormalizeLineBreaks(innerMatcher);
  }

}
