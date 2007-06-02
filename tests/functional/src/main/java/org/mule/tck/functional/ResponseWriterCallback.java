/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.functional;

import org.mule.umo.UMOEventContext;

/**
 * A test callback that writes the results of a service invocation to the response output stream
 * of the event
 * This should only be used when testing Asynchronous calls with the {@link FunctionalTestComponent} otherwise
 * you will get duplicate messages, since both this class and the {@link FunctionalTestComponent} will write
 * a return message back to the callee.
 *
 * @see org.mule.tck.functional.FunctionalTestComponent
 */
public class ResponseWriterCallback extends CounterCallback
{

    public void eventReceived(UMOEventContext context, Object component) throws Exception
    {
        super.eventReceived(context, component);

        String result = context.getMessageAsString() + " Received Async";
        assert (context.getOutputStream() != null);

        context.getOutputStream().write(result.getBytes());
        context.getOutputStream().flush();
    }
}
