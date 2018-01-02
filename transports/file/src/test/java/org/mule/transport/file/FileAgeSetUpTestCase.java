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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.transport.AbstractConnector.PROPERTY_POLLING_FREQUENCY;
import static org.mule.transport.file.FileConnector.FILE;
import static org.mule.transport.file.FileConnector.PROPERTY_FILE_AGE;
import static org.mule.transport.file.FileConnector.PROPERTY_MOVE_TO_DIRECTORY;
import static org.mule.transport.file.FileConnector.PROPERTY_MOVE_TO_PATTERN;
import static org.mule.transport.file.FileConnector.PROPERTY_READ_FROM_DIRECTORY;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.registry.AbstractServiceDescriptor;
import org.mule.api.transport.SessionHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.service.TransportServiceDescriptor;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class FileAgeSetUpTestCase extends AbstractMuleContextTestCase
{

    private FileConnector connector;

    @Override
    protected void doSetUp() throws Exception
    {
        mockServiceDescriptor();
        connector = createConnector();
        muleContext.start();
        connector.initialise();
    }

    @Test
    public void getFileAgeInRestart() throws Exception
    {
        FlowConstruct flowConstruct = mock(FlowConstruct.class);
        InboundEndpoint endpoint = mock(InboundEndpoint.class, RETURNS_DEEP_STUBS);
        Map<String, String> properties = mockEndpointProperties(endpoint);

        connector.createReceiver(flowConstruct, endpoint);

        assertThat(properties.get(PROPERTY_FILE_AGE), is(instanceOf(Long.class)));

        connector.createReceiver(flowConstruct, endpoint);
    }

    public FileConnector createConnector()
    {
        FileConnector connector = new FileConnector(muleContext);
        connector.setName("testFile");
        connector.setOutputAppend(true);
        return connector;
    }

    private Map<String, String> mockEndpointProperties(InboundEndpoint endpoint)
    {
        Map<String, String> properties = new HashMap<>();
        properties.put(PROPERTY_READ_FROM_DIRECTORY, "");
        properties.put(PROPERTY_MOVE_TO_DIRECTORY, "");
        properties.put(PROPERTY_MOVE_TO_PATTERN, "");
        properties.put(PROPERTY_POLLING_FREQUENCY, "10000");
        properties.put(PROPERTY_FILE_AGE, "10000");
        when(endpoint.getProperties()).thenReturn(properties);
        return properties;
    }

    private void mockServiceDescriptor() throws Exception
    {
        SessionHandler sessionHandler = mock(SessionHandler.class);
        TransportServiceDescriptor transportServiceDescriptor = mock(TransportServiceDescriptor.class);
        when(transportServiceDescriptor.createSessionHandler()).thenReturn(sessionHandler);
        muleContext.getRegistry().registerObject(new AbstractServiceDescriptor.Key(FILE, null).getKey(), transportServiceDescriptor);
    }

}
