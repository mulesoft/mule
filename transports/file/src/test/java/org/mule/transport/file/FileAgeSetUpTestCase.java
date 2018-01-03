/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.transport.AbstractConnector.PROPERTY_POLLING_FREQUENCY;
import static org.mule.transport.file.FileConnector.FILE;
import static org.mule.transport.file.FileConnector.PROPERTY_FILE_AGE;
import static org.mule.transport.file.FileConnector.PROPERTY_MOVE_TO_DIRECTORY;
import static org.mule.transport.file.FileConnector.PROPERTY_MOVE_TO_PATTERN;
import static org.mule.transport.file.FileConnector.PROPERTY_READ_FROM_DIRECTORY;
import org.mule.MessageExchangePattern;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.AbstractServiceDescriptor;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.SessionHandler;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.service.TransportServiceDescriptor;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class FileAgeSetUpTestCase extends AbstractMuleContextTestCase
{

    private FileConnector connector;
    private InboundEndpoint endpoint;

    @Override
    protected void doSetUp() throws Exception
    {
        mockServiceDescriptor();
        connector = createConnector();
        muleContext.start();
        connector.initialise();
        endpoint = getEndpoint();
    }

    @Test
    public void getFileAgeInRestart() throws Exception
    {
        endpoint.start();

        assertThat(endpoint.getProperties().get(PROPERTY_FILE_AGE), is(instanceOf(Long.class)));

        endpoint.stop();
        endpoint.start();

        assertThat(endpoint.getProperties().get(PROPERTY_FILE_AGE), is(instanceOf(Long.class)));
    }

    public FileConnector createConnector()
    {
        FileConnector connector = new FileConnector(muleContext);
        connector.setName("testFile");
        connector.setOutputAppend(true);
        return connector;
    }

    private Map<String, String> getEndpointProperties()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put(PROPERTY_READ_FROM_DIRECTORY, "");
        properties.put(PROPERTY_MOVE_TO_DIRECTORY, "");
        properties.put(PROPERTY_MOVE_TO_PATTERN, "");
        properties.put(PROPERTY_POLLING_FREQUENCY, "10000");
        properties.put(PROPERTY_FILE_AGE, "10000");
        return properties;
    }

    private void mockServiceDescriptor() throws Exception
    {
        SessionHandler sessionHandler = mock(SessionHandler.class);
        TransportServiceDescriptor transportServiceDescriptor = mock(TransportServiceDescriptor.class);
        when(transportServiceDescriptor.createSessionHandler()).thenReturn(sessionHandler);
        when(transportServiceDescriptor.createMessageReceiver(any(FileConnector.class), any(FlowConstruct.class), any(InboundEndpoint.class), anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(mock(MessageReceiver.class));
        muleContext.getRegistry().registerObject(new AbstractServiceDescriptor.Key(FILE, null).getKey(), transportServiceDescriptor);
    }

    private InboundEndpoint getEndpoint() throws Exception
    {
        EndpointURI endpointURI = mock(EndpointURI.class);
        when(endpointURI.getAddress()).thenReturn("file://test");
        EndpointMessageProcessorChainFactory endpointMessageProcessorChainFactory = mock(EndpointMessageProcessorChainFactory.class);
        when(endpointMessageProcessorChainFactory.createInboundMessageProcessorChain(any(InboundEndpoint.class), any(FlowConstruct.class), any(MessageProcessor.class))).thenReturn(mock(MessageProcessor.class));
        InboundEndpoint endpoint = new DefaultInboundEndpoint(connector, endpointURI, null, getEndpointProperties(), null,
                                                              false, MessageExchangePattern.ONE_WAY, 42, null, null, null,
                                                              muleContext, null, null, endpointMessageProcessorChainFactory, null, null, false, null);

        endpoint.setFlowConstruct(mock(FlowConstruct.class));
        return endpoint;
    }

}
