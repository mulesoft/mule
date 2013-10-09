/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;

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

import org.junit.Before;
import org.junit.Test;

public class HttpMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    private static final String CONTEXT_PATH = "/resources";
    private static final String CLIENT_PATH = "/resources/client";
    private static final String CLIENT_NAME_PATH = "/resources/client/name";

    private HttpMessageReceiver httpMessageReceiver;

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

    @Before
    public void setUp() throws Exception
    {
        httpMessageReceiver = (HttpMessageReceiver) getMessageReceiver();
    }


    @Test
    public void testProcessResourceRelativePath()
    {
        assertEquals("client", httpMessageReceiver.processRelativePath(CONTEXT_PATH, CLIENT_PATH));
    }
    
    @Test
    public void testProcessRelativePathSameLevel()
    {
        assertEquals("", httpMessageReceiver.processRelativePath(CONTEXT_PATH, CONTEXT_PATH));
    }
    
    @Test
    public void testProcessResourcePropertyRelativePath()
    {
        assertEquals("client/name", httpMessageReceiver.processRelativePath(CONTEXT_PATH, CLIENT_NAME_PATH));
    }

}
