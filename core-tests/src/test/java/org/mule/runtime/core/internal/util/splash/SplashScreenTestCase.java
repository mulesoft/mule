/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.splash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class SplashScreenTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testMuleContextSplashScreenRendering() throws Exception {
    SplashScreen serverStartupSplashScreen = new ServerStartupSplashScreen();
    assertNotNull(serverStartupSplashScreen);
    assertTrue(serverStartupSplashScreen.toString().length() > 0);

    muleContext.start();
    muleContext.stop();
    String initialStartBoilerPlate = serverStartupSplashScreen.toString();

    muleContext.start();
    muleContext.stop();
    String subsequentStartBoilerPlate = serverStartupSplashScreen.toString();

    // Only lightly validate on size because content changes, e.g. server start time-stamp
    assertEquals("Splash-screen sizes differ, ", initialStartBoilerPlate.length(), subsequentStartBoilerPlate.length());
  }

}
