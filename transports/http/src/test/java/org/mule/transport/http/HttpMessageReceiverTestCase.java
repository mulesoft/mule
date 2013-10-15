/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.transport.AbstractMessageReceiverTestCase;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;
import org.mule.util.CollectionUtils;

import com.mockobjects.dynamic.Mock;

public class HttpMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Mock mockComponent = new Mock(Service.class);
        mockComponent.expect("getResponseRouter");
        mockComponent.expectAndReturn("getInboundRouter", new ServiceCompositeMessageSource());

        return new HttpMessageReceiver(endpoint.getConnector(), (Service) mockComponent.proxy(), endpoint);
    }

    public InboundEndpoint getEndpoint() throws Exception
    {
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("http://localhost:6789", muleContext);
        endpointBuilder.setResponseTransformers(CollectionUtils.singletonList(new MuleMessageToHttpResponse()));
        endpoint = muleContext.getEndpointFactory().getInboundEndpoint(endpointBuilder);
        return endpoint;
    }
}
