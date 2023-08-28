/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.infrastructure.process.matchers;

import org.mule.test.infrastructure.process.MuleProcessController;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class Running extends TypeSafeMatcher<MuleProcessController> {

  private final boolean isRunning;

  public Running(boolean status) {
    this.isRunning = status;
  }

  @Override
  public boolean matchesSafely(MuleProcessController mule) {
    try {
      return isRunning == mule.isRunning();
    } catch (Error e) {
      return false;
    }
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a Mule Standalone server that is " + (isRunning ? "" : "not ") + "running");
  }

  public static <T> Matcher<MuleProcessController> isRunning() {
    return new Running(true);
  }

  public static <T> Matcher<MuleProcessController> notRunning() {
    return new Running(false);
  }

};
