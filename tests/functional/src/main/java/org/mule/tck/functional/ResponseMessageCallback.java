/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.functional;

import org.mule.umo.UMOEventContext;

/**
 * A test callback that writes the results of a service invocation to the response output stream
 * of the event
 * This should only be used when testing Asynchronous calls with the {@link org.mule.tck.functional.FunctionalTestComponent} otherwise
 * you will get duplicate messages, since both this class and the {@link org.mule.tck.functional.FunctionalTestComponent} will write
 * a return message back to the callee.
 *
 * @see FunctionalTestComponent
 */
public class ResponseMessageCallback extends CounterCallback
{

    public void eventReceived(UMOEventContext context, Object component) throws Exception
    {
        if (context.isSynchronous())
        {
            throw new IllegalStateException("The ResponseWriterCallback should not be used for synchronous tests as it will cause two copies of the message to be written back to the client");
        }
        super.eventReceived(context, component);

        String result = context.getMessageAsString() + " Received Async";
        context.dispatchEvent(result);
    }

}