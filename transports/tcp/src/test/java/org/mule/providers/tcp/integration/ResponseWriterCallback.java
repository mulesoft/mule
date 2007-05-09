/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.tcp.integration;

import org.mule.tck.functional.EventCallback;
import org.mule.umo.UMOEventContext;

/**
 * A test callback that writes the results of a service invocation to the response output stream
 * of the event
 */
public class ResponseWriterCallback implements EventCallback
{
    private int callbackCount = 0;

    public void eventReceived(UMOEventContext context, Object Component) throws Exception
    {
        incCallbackCount();
        String result = context.getMessageAsString() + " Received Async";
        assert (context.getOutputStream() != null);

        context.getOutputStream().write(result.getBytes());
        context.getOutputStream().flush();
    }

    protected synchronized void incCallbackCount()
    {
        callbackCount = callbackCount + 1;
    }

    public boolean isCallbackCalled()
    {
        return callbackCount > 0;
    }

    public int getCallbackCount()
    {
        return callbackCount;
    }
}
