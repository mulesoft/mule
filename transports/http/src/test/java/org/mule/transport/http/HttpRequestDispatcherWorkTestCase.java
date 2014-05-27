/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.execution.MessageProcessContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.resource.spi.work.Work;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class HttpRequestDispatcherWorkTestCase extends AbstractMuleTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpConnector mockHttpConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Socket mockSocket;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpMessageReceiver mockHttpMessageReceiver;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Work mockWork;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessContext mockMessageContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    NoReceiverForEndpointException mockNoReceiverForEndpointException;

    @Test(expected = IllegalArgumentException.class)
    public void createHttpRequestDispatcherWorkWithNullHttpConnector()
    {
        new HttpRequestDispatcherWork(null, mockSocket);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createHttpRequestDispatcherWorkWithNullServerSocket()
    {
        new HttpRequestDispatcherWork(mockHttpConnector, null);
    }

    @Test
    public void onExceptionCallSystemExceptionHandler() throws Exception
    {
        HttpRequestDispatcherWork httpRequestDispatcherWork = new HttpRequestDispatcherWork(mockHttpConnector, mockSocket);
        setUpSocketMessage();
        when(mockHttpConnector.lookupReceiver(any(Socket.class), any(RequestLine.class))).thenThrow(Exception.class);
        httpRequestDispatcherWork.run();
        verify(mockHttpConnector.getMuleContext().getExceptionListener(), times(1)).handleException(any(Exception.class));
    }

    @Test
    public void requestPathWithNoReceiver() throws Exception
    {
        HttpRequestDispatcherWork httpRequestDispatcherWork = new HttpRequestDispatcherWork(mockHttpConnector, mockSocket);
        setUpSocketMessage();
        when(mockSocket.getLocalSocketAddress()).thenReturn(new InetSocketAddress(0));
        when(mockHttpConnector.lookupReceiver(any(Socket.class), any(RequestLine.class))).thenThrow(mockNoReceiverForEndpointException);
        ByteArrayOutputStream socketOutput = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(socketOutput);
        httpRequestDispatcherWork.run();
        String response = new String(socketOutput.toByteArray());
        assertThat(response.startsWith("HTTP/1.0 404 Not Found"), Is.is(true));
    }

    @Test
    public void onValidUriProcessRequest() throws Exception
    {
        HttpRequestDispatcherWork httpRequestDispatcherWork = new HttpRequestDispatcherWork(mockHttpConnector, mockSocket);
        when(mockHttpConnector.lookupReceiver(isA(Socket.class), isA(RequestLine.class))).thenReturn(mockHttpMessageReceiver);
        setUpSocketMessage();
        when(mockHttpMessageReceiver.createMessageProcessContext()).thenReturn(mockMessageContext);
        httpRequestDispatcherWork.run();
        verify(mockHttpMessageReceiver, times(1)).processRequest(isA(HttpServerConnection.class));
    }

    private void setUpSocketMessage() throws IOException
    {
        when(mockHttpConnector.getMuleContext().getConfiguration().getDefaultEncoding()).thenReturn("UTF-8");
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream("GET /path/to/file/index.html HTTP/1.0\n".getBytes()));
    }

}
