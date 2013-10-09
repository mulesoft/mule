/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck;

import org.mule.work.DefaultWorkListener;

import javax.resource.spi.work.WorkEvent;

public class TestingWorkListener extends DefaultWorkListener
{
    protected void handleWorkException(WorkEvent event, String type)
    {
        super.handleWorkException(event, type);
        if (event.getException() != null)
        {
            Throwable t = event.getException().getCause();
            if (t != null)
            {

                if (t instanceof Error)
                {
                    throw (Error)t;
                }
                else if (t instanceof RuntimeException)
                {
                    throw (RuntimeException)t;
                }
            }

        }
    }
}
