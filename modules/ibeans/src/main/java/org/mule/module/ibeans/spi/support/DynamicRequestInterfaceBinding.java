/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
            throw new MessagingException(CoreMessages.failedToInvoke("inboundEndpoint.request()"), event, e, this);
        }
    }
}
