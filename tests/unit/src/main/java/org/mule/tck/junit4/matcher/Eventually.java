/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import org.mule.tck.probe.Timeout;

import java.util.concurrent.TimeUnit;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

/**
 * Should the matcher be satisfied with delay?
 */
public class Eventually<T> extends BaseMatcher {

  private final Matcher<T> matcher;
  private TimeUnit timeUnit = TimeUnit.SECONDS;
  private int time = 10;
  private long interval = 1000;

  /**
   * Creates a matcher that retry the matcher until it is satisfied or until the timeout.
   *
   * @param matcher the matcher that will be executed until the timeout
   */
  @Factory
  public static <T> Eventually<T> eventually(Matcher<T> matcher) {
    return new Eventually<T>(matcher);
  }

  private Eventually(Matcher<T> matcher) {
    this.matcher = matcher;
  }

  /**
   * Configures the time limit for waiting for the matcher condition before failing.
   */
  public Eventually<T> atMostIn(int time, TimeUnit timeUnit) {
    this.time = time;
    this.timeUnit = timeUnit;
    return this;
  }

  /**
   * Configures the repetition interval for retrying the matcher condition.
   */
  public Eventually<T> every(int time, TimeUnit timeUnit) {
    this.interval = timeUnit.toMillis(time);
    return this;
  }

  @Override
  public boolean matches(Object item) {
    Timeout timeout = new Timeout(timeUnit.toMillis(time));
    while (!matcher.matches(item)) {
      if (timeout.hasTimedOut()) {
        return false;
      }
      try {
        Thread.sleep(interval);
      } catch (InterruptedException e) {
        new RuntimeException("Interrupted while waiting for condition", e);
      }
    }
    return true;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("after " + time + " " + timeUnit.toString() + " s").appendDescriptionOf(matcher);
  }
}
