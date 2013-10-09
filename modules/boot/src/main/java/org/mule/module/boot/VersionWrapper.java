/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.boot;

import org.mule.config.i18n.CoreMessages;

import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * There is a need to exit from wrapper
 * This is a good place for other information message
 */
public class VersionWrapper implements WrapperListener
{
    public Integer start(String[] args)
    {
        try
        {
            System.out.println(CoreMessages.productInformation());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Integer.valueOf(1);
        }
        return null;
    }

    public int stop(int exitCode)
    {
        return exitCode;
    }

    public void controlEvent(int event)
    {
        if (WrapperManager.isControlledByNativeWrapper())
        {
            // The Wrapper will take care of this event
        }
        else
        {
            // We are not being controlled by the Wrapper, so
            //  handle the event ourselves.
            if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) ||
                (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) ||
                (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT))
            {
                WrapperManager.stop(0);
            }
        }
    }
}
