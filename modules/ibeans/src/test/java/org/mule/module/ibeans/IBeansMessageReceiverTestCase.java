/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.module.ibeans.config.IBeanHolderConfigurationBuilder;
import org.mule.transport.AbstractMessageReceiverTestCase;
import org.mule.transport.ibean.IBeansMessageReceiver;

import com.mockobjects.dynamic.Mock;

import java.util.List;

public class IBeansMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        IBeanHolderConfigurationBuilder builder = new IBeanHolderConfigurationBuilder("org.mule");
        builders.add(builder);
    }

    @Override
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Mock mockService = new Mock(Service.class);
        mockService.expect("dispose");
        return new IBeansMessageReceiver(endpoint.getConnector(), (Service)mockService.proxy(), endpoint);
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        String url = "ibean://hostip.getHostInfo";
        return muleContext.getEndpointFactory().getInboundEndpoint(url);
    }
}
