/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.umo.manager.DefaultWorkListener;

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
