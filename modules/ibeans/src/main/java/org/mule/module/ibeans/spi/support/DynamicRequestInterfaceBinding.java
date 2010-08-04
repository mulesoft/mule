/*
 * $Id: DynamicRequestInterfaceBinding.java 174 2009-11-07 20:36:32Z ross $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi.support;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.config.i18n.CoreMessages;

import org.ibeans.api.channel.CHANNEL;

/**
 * Creates an component binding that can use the {@link org.mule.api.transport.MessageRequester} interface to make
 * a call.  The need for this class is that the MessageRequester has no support for passing an {@link org.mule.api.MuleMessage} so this
 * binding will set the message on the endpoint and use it when the request is made
 */
public class DynamicRequestInterfaceBinding extends DefaultRequestInterfaceBinding
{
    @Override
    public MuleMessage route(MuleMessage message, MuleSession session) throws MessagingException
    {
        try
        {
            int timeout = message.getInboundProperty(CHANNEL.TIMEOUT, getMuleContext().getConfiguration().getDefaultResponseTimeout());
            if (inboundEndpoint instanceof DynamicRequestEndpoint)
            {
                return ((DynamicRequestEndpoint) inboundEndpoint).request(timeout, message);
            }
            else
            {
                return inboundEndpoint.request(getMuleContext().getConfiguration().getDefaultResponseTimeout());
            }
        }
        catch (Exception e)
        {
            throw new MessagingException(CoreMessages.failedToInvoke("inboundEndpoint.request()"), message, e);
        }
    }
}
