/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import java.io.File;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

public class VerifyLogWithMatcher extends TypeSafeMatcher<MuleProcessController> {

  private String app;
  private Matcher<File> fileMatcher;

  @Factory
  public static Matcher<MuleProcessController> log(Matcher<File> matcher) {
    return new VerifyLogWithMatcher(matcher);
  }

  @Factory
  public static Matcher<MuleProcessController> log(String appName, Matcher<File> matcher) {
    return new VerifyLogWithMatcher(appName, matcher);
  }

  private VerifyLogWithMatcher(Matcher<File> matcher) {
    fileMatcher = matcher;
  }

  private VerifyLogWithMatcher(String appName, Matcher<File> matcher) {
    this.app = appName;
    fileMatcher = matcher;
  }

  @Override
  public boolean matchesSafely(MuleProcessController muleProcessController) {
    File logFile = (app == null) ? muleProcessController.getLog() : muleProcessController.getLog(app);
    return fileMatcher.matches(logFile);
  }


  @Override
  public void describeTo(Description description) {
    String message = (app == null) ? "mule log that has " : String.format("%s application log that have ", app);
    description.appendText(message).appendDescriptionOf(fileMatcher);
  }
}
