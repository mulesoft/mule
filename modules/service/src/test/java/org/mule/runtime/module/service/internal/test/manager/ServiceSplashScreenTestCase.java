/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.manager;

import static java.lang.System.lineSeparator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.module.service.internal.manager.ServiceSplashScreen;

import org.junit.Test;

public class ServiceSplashScreenTestCase {

  @Test
  public void splashScreen() {
    // When: the splash screen is created with a title and a message
    String message = "one" + lineSeparator() + "two" + lineSeparator() + "three";
    ServiceSplashScreen serviceSplashScreen = new ServiceSplashScreen("Test Case", message);

    // Then: the splash screen is formatted correctly
    assertThat(serviceSplashScreen.toString(), is(lineSeparator() +
        "**********************************************************************" + lineSeparator() +
        "* Started Test Case                                                  *" + lineSeparator() +
        "*                                                                    *" + lineSeparator() +
        "* one                                                                *" + lineSeparator() +
        "* two                                                                *" + lineSeparator() +
        "* three                                                              *" + lineSeparator() +
        "**********************************************************************"));
  }

}
