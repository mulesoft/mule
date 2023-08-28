/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.infrastructure.process.matchers;

import org.mule.test.infrastructure.process.MuleProcessController;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class Deployment extends TypeSafeMatcher<MuleProcessController> {

  private final String applicationName;

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

  public static <T> Matcher<MuleProcessController> hasDeployed(String applicationName) {
    return new Deployment(applicationName);
  }

};
