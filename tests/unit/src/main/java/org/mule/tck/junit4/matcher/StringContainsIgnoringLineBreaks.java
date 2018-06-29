/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.matcher;

import org.hamcrest.Factory;
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

  @Override
  protected String relationship() {
    return "containing ignoring line breaks ";
  }

  @Factory
  public static Matcher<String> containsStringIgnoringLineBreaks(String substring) {
    return new StringContainsIgnoringLineBreaks(substring);
  }
}
