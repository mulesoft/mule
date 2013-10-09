/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SplashScreenTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testMuleContextSplashScreenRendering() throws Exception
    {
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
