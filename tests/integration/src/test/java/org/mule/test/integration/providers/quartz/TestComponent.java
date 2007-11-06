/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.quartz;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

public class TestComponent implements Callable
{

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        if (eventContext.getMessageAsString().equals("quartz test"))
        {
            QuartzDispatchJobTestCase.countDown.countDown();
            QuartzReceiveAndDispatchJobTestCase.countDown.countDown();
            QuartzReceiveAndDispatchUsingDelegatingJobTestCase.countDown.countDown();
        }
        else
        {
            throw new IllegalArgumentException("Unrecognised event payload");
        }
        return null;
    }

}
