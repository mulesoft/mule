/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
