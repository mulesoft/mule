/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.EndpointException;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.ConnectException;

import java.io.IOException;
import java.net.URI;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class HttpConnectionManagerTestCase extends AbstractMuleTestCase
{

    public static final String DEFAULT_ENDPOINT_URI = "http://localhost:1234/";
    public static final String NESTED_ENDPOINT_URI_1 = "http://localhost:1234/service";
    public static final String NESTED_ENDPOINT_URI_2 = "http://localhost:1234/service/order";
    public static final String ANOTHER_ENDPOINT_URI = "http://localhost:1235/service";
    public static final String ANOTHER_NESTED_ENDPOINT_URI = "http://localhost:1235/service/order";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WorkManager mockWorkManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpConnector mockHttpConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleContext mockMuleContext;

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullWorkManager()
    {
        new HttpConnectionManager(mockHttpConnector, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithNullConnector()
    {
        new HttpConnectionManager(null, mockWorkManager);
    }

    @Test(expected = MuleRuntimeException.class)
    public void workSchedulingFails() throws Exception
    {
        when(mockHttpConnector.getServerSocket(any(URI.class))).thenThrow(IOException.class);
        createConnectionManagerAndAddDefaultEndpointUri();
    }

    @Test
    public void removeConnectionWithoutDispatcherDoesntFail() throws Exception
    {
        HttpConnectionManager connectionManager = new HttpConnectionManager(mockHttpConnector, mockWorkManager);
        connectionManager.removeConnection(new MuleEndpointURI("http://localhost:1234/service/path", mockMuleContext));
    }

    @Test
    public void addConnectionStartsSocketDispatcher() throws Exception
    {
        createConnectionManagerAndAddDefaultEndpointUri();
        verify(mockWorkManager, times(1)).scheduleWork(any(HttpRequestDispatcher.class), anyLong(), any(ExecutionContext.class), any(WorkListener.class));
    }

    @Test
    public void add3EndpointUrisToSameHostPortOnlyExecutesOneDispatcher() throws Exception
    {
        HttpConnectionManager connectionManager = createConnectionManagerAndAddDefaultEndpointUri();
        connectionManager.addConnection(createEndpointUri(NESTED_ENDPOINT_URI_1));
        connectionManager.addConnection(createEndpointUri(NESTED_ENDPOINT_URI_2));
        verify(mockWorkManager, times(1)).scheduleWork(any(HttpRequestDispatcher.class), anyLong(), any(ExecutionContext.class), any(WorkListener.class));
    }

    @Test
    public void addEndpointsToDifferentHostPortOpensSeveralConnections() throws Exception
    {
        HttpConnectionManager connectionManager = createConnectionManagerAndAddDefaultEndpointUri();
        connectionManager.addConnection(createEndpointUri(NESTED_ENDPOINT_URI_1));
        connectionManager.addConnection(createEndpointUri(NESTED_ENDPOINT_URI_2));
        connectionManager.addConnection(createEndpointUri(ANOTHER_ENDPOINT_URI));
        connectionManager.addConnection(createEndpointUri(ANOTHER_NESTED_ENDPOINT_URI));
        verify(mockWorkManager, times(2)).scheduleWork(any(HttpRequestDispatcher.class), anyLong(), any(ExecutionContext.class), any(WorkListener.class));
    }

    @Test
    public void disposeShutdownsEverything() throws Exception
    {
        HttpConnectionManager connectionManager = createConnectionManagerAndAddDefaultEndpointUri();
        connectionManager.dispose();
        verify(mockWorkManager, times(1)).dispose();
    }

    private HttpConnectionManager createConnectionManagerAndAddDefaultEndpointUri() throws ConnectException, EndpointException
    {
        HttpConnectionManager connectionManager = new HttpConnectionManager(mockHttpConnector, mockWorkManager);
        connectionManager.addConnection(createEndpointUri(DEFAULT_ENDPOINT_URI));
        return connectionManager;
    }

    private MuleEndpointURI createEndpointUri(String uri) throws EndpointException
    {
        return new MuleEndpointURI(uri, mockMuleContext);
    }


}
