/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    public MuleEvent execute(ExecutionCallback<MuleEvent> callback) throws Exception
    {
        try
        {
            return next.execute(callback);
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