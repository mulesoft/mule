/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
