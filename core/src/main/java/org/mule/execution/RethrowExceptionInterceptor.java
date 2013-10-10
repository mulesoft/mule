/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.execution;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.execution.ExecutionCallback;

class RethrowExceptionInterceptor implements ExecutionInterceptor<MuleEvent>
{

    private final ExecutionInterceptor<MuleEvent> next;

    public RethrowExceptionInterceptor(ExecutionInterceptor<MuleEvent> next)
    {
        this.next = next;
    }

    @Override
    public MuleEvent execute(ExecutionCallback<MuleEvent> processingCallback) throws Exception
    {
        try
        {
            return this.next.execute(processingCallback);
        }
        catch (MessagingException e)
        {
            if (e.handled())
            {
                return e.getEvent();
            }
            throw e;
        }
    }
}
