/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    public MuleEvent execute(ExecutionCallback<MuleEvent> processingCallback, ExecutionContext executionContext) throws Exception
    {
        try
        {
            return this.next.execute(processingCallback, executionContext);
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
