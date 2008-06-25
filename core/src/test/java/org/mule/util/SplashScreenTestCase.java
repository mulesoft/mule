/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.tck.AbstractMuleTestCase;

public class SplashScreenTestCase extends AbstractMuleTestCase
{

    public void testMuleContextSplashScreenRendering() throws Exception
    {
        SplashScreen serverStartupSplashScreen = SplashScreen.getInstance(ServerStartupSplashScreen.class);
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
