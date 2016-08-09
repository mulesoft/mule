/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.matchers;

import org.mule.test.infrastructure.process.MuleProcessController;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class Deployment extends TypeSafeMatcher<MuleProcessController> {

  private String applicationName;

  public Deployment(String applicationName) {
    this.applicationName = applicationName;
  }

  @Override
  public boolean matchesSafely(MuleProcessController mule) {
    return mule.isDeployed(applicationName);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a Mule Standalone server that has successfully deployed: " + applicationName);
  }

  @Factory
  public static <T> Matcher<MuleProcessController> hasDeployed(String applicationName) {
    return new Deployment(applicationName);
  }

};
