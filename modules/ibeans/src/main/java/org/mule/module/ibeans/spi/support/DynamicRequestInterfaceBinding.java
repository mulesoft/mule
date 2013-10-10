/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi.support;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.NullPayload;

import org.ibeans.api.channel.CHANNEL;

/**
 * Creates an component binding that can use the {@link org.mule.api.transport.MessageRequester} interface to make
 * a call.  The need for this class is that the MessageRequester has no support for passing an {@link org.mule.api.MuleMessage} so this
 * binding will set the message on the endpoint and use it when the request is made
 */
public class DynamicRequestInterfaceBinding extends DefaultRequestInterfaceBinding
{
    @Override
    public MuleEvent process(MuleEvent event) throws MessagingException
    {
        try
        {
            int timeout = event.getMessage().getInboundProperty(CHANNEL.TIMEOUT, event.getMuleContext().getConfiguration().getDefaultResponseTimeout());
            if (inboundEndpoint instanceof DynamicRequestEndpoint)
            {
                MuleMessage message =((DynamicRequestEndpoint) inboundEndpoint).request(timeout, event);
                if(message == null)
                {
                    message = new DefaultMuleMessage(NullPayload.getInstance(), event.getMuleContext());
                }
                return new DefaultMuleEvent(message, event);
            }
            else
            {
                return new DefaultMuleEvent(inboundEndpoint.request(event.getMuleContext()
                    .getConfiguration()
                    .getDefaultResponseTimeout()), event);
            }
        }
        catch (Exception e)
        {
            throw new MessagingException(CoreMessages.failedToInvoke("inboundEndpoint.request()"), event, e);
        }
    }
}
