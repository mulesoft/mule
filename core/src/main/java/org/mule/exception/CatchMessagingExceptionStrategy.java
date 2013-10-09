/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.exception;

import org.mule.api.MuleEvent;

public class CatchMessagingExceptionStrategy extends TemplateMessagingExceptionStrategy
{
    public CatchMessagingExceptionStrategy()
    {
        setHandleException(true);
    }

    @Override
    protected void nullifyExceptionPayloadIfRequired(MuleEvent event)
    {
        event.getMessage().setExceptionPayload(null);
    }

    @Override
    protected MuleEvent afterRouting(Exception exception, MuleEvent event)
    {
        return event;
    }

    @Override
    protected MuleEvent beforeRouting(Exception exception, MuleEvent event)
    {
        return event;
    }

}
