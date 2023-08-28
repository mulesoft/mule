/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.junit4.matcher;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;

/**
 * Tests if the argument is a string that contains a substring but ignoring line breaks
 */
public class StringContainsIgnoringLineBreaks extends StringContains {

  public StringContainsIgnoringLineBreaks(String substring) {
    super(substring);
  }

  @Override
  protected boolean evalSubstringOf(String s) {
    String sWithoutLineBreaks = s.replace("\r\n", "").replace("\n", "");
    String substringWithoutLineBreaks = substring.replace("\r\n", "").replace("\n", "");
    return sWithoutLineBreaks.indexOf(substringWithoutLineBreaks) >= 0;
  }

  public static Matcher<String> containsStringIgnoringLineBreaks(String substring) {
    return new StringContainsIgnoringLineBreaks(substring);
  }
}
