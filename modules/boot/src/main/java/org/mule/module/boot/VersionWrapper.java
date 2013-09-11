/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
