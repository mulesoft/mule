/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

public class TestComponent2 implements Callable
{

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        if (eventContext.getMessageAsString().equals("quartz test"))
        {
            // TODO - fix this lazy static mess
            if (null != QuartzDispatchJobTestCase.countDown)
            {
                QuartzDispatchJobTestCase.countDown.countDown();
            }
            if (null != QuartzReceiveAndDispatchJobTestCase.countDown)
            {
                QuartzReceiveAndDispatchJobTestCase.countDown.countDown();
            }
            if (null != QuartzReceiveAndDispatchUsingDelegatingJobTestCase.countDown)
            {
                QuartzReceiveAndDispatchUsingDelegatingJobTestCase.countDown.countDown();
            }
        }
        else
        {
            throw new IllegalArgumentException("Unrecognised event payload");
        }
        return null;
    }

}
