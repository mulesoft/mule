/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
