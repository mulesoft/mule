/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.api.MuleContext;
import org.mule.config.i18n.CoreMessages;

import java.util.Date;

public class ServerShutdownSplashScreen extends SplashScreen
{
    protected void doHeader(MuleContext context)
    {
        long currentTime = System.currentTimeMillis();
        header.add(CoreMessages.shutdownNormally(new Date()).getMessage());
        long duration = 10;
        if (context.getStartDate() > 0)
        {
            duration = currentTime - context.getStartDate();
        }
        header.add(CoreMessages.serverWasUpForDuration(duration).getMessage());
    }
}


