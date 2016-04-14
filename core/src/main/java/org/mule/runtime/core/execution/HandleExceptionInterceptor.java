/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.execution.ExecutionCallback;

class HandleExceptionInterceptor implements ExecutionInterceptor<MuleEvent>
{
    final private ExecutionInterceptor<MuleEvent> next;
    private MessagingExceptionHandler messagingExceptionHandler;

    public HandleExceptionInterceptor(ExecutionInterceptor<MuleEvent> next, MessagingExceptionHandler messagingExceptionHandler)
    {
        this.next = next;
        this.messagingExceptionHandler = messagingExceptionHandler;
    }

    @Override
    public MuleEvent execute(ExecutionCallback<MuleEvent> callback, ExecutionContext executionContext) throws Exception
    {
        try
        {
            return next.execute(callback, executionContext);
        }
        catch (MessagingException e)
        {
            MuleEvent result;
            if (messagingExceptionHandler != null)
            {
                result = messagingExceptionHandler.handleException(e, e.getEvent());
            }
            else
            {
                result = e.getEvent().getFlowConstruct().getExceptionListener().handleException(e,e.getEvent());
            }
            e.setProcessedEvent(result);
            throw e;
        }
        catch (Exception e)
        {
            throw e;
        }
    }
}