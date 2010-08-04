/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ibeans;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractMessageReceiverTestCase;
import org.mule.transport.ibean.IBeansMessageReceiver;

import com.mockobjects.dynamic.Mock;

public class IBeansMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    /* For general guidelines on writing transports see
       http://www.mulesoft.org/display/MULE2USER/Creating+Transports */

    @Override
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Mock mockService = new Mock(Service.class);
        mockService.expectAndReturn("getResponseTransformer", null);
        return new IBeansMessageReceiver(endpoint.getConnector(), (Service) mockService.proxy(), endpoint);
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
         return muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("ibean://hostip.getHostInfo");
    }
}
