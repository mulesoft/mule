/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
