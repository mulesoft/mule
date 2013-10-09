/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty.functional;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.transport.http.HttpConnector;

public class PreservePayloadExceptionStrategy extends AbstractMessagingExceptionStrategy
{
    public PreservePayloadExceptionStrategy(MuleContext muleContext)
    {
        super(muleContext);
    }

    private MuleEvent processException(Exception e, MuleEvent event, RollbackSourceCallback rollbackCallback)
    {
        Object payloadBeforeException = event.getMessage().getPayload();
        event.getMessage().setPayload(payloadBeforeException);
        event.getMessage().setExceptionPayload(null);
        event.getMessage().setOutboundProperty("CustomES", "CustomES");
        event.getMessage().setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, "400");
        return event;
    }

    @Override
    public MuleEvent handleException(Exception e, MuleEvent event)
    {
        return processException(e, event, null);
    }
}
