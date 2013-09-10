/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.functional;

import org.mule.api.MuleEventContext;

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
    @Override
    public void eventReceived(MuleEventContext context, Object component) throws Exception
    {
        if (context.getExchangePattern().hasResponse())
        {
            throw new IllegalStateException("The ResponseWriterCallback should not be used for synchronous tests as it will cause two copies of the message to be written back to the client");
        }
        super.eventReceived(context, component);

        String result = context.getMessageAsString() + " Received Async";
        if (context.getOutputStream() == null)
        {
            throw new IllegalArgumentException("event context does not have an OutputStream associated");
        }

        context.getOutputStream().write(result.getBytes());
        context.getOutputStream().flush();
    } 
}
