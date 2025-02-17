/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.manager;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.mule.runtime.module.service.internal.manager.ServiceSplashScreen;

public class ServiceSplashScreenTestCase {

  @Test
  public void splashScreen() {
    ServiceSplashScreen serviceSplashScreen = new ServiceSplashScreen("Test Case", "one\ntwo\nthree");
    assertThat(serviceSplashScreen.toString(), is("\n" +
        "**********************************************************************\n" +
        "* Started Test Case                                                  *\n" +
        "*                                                                    *\n" +
        "* one                                                                *\n" +
        "* two                                                                *\n" +
        "* three                                                              *\n" +
        "**********************************************************************"));
  }

}
