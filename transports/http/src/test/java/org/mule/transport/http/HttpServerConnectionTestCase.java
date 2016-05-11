/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.size.SmallTest;
import org.mule.transport.tcp.TcpConnector;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.HashMap;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;

import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class HttpServerConnectionTestCase extends AbstractMuleContextTestCase
{

    private final static boolean SEND_TCP_NO_DELAY = false;
    private final static boolean KEEP_ALIVE = true;
    private final static int SERVER_SO_TIMEOUT = 5000;

    public DynamicPort port1 = new DynamicPort("port1");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Socket mockSocket;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SSLSocket mockSslSocket;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpConnector mockHttpConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HandshakeCompletedEvent mockHandshakeCompleteEvent;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpRequest mockHttpRequest;
    private Certificate[] mockLocalCertificate = new Certificate[2];
    private Certificate[] mockPeerCertificates = new Certificate[2];

    @Test
    public void createHttpServerConnectionWithSslSocket() throws IOException
    {
        when(mockHandshakeCompleteEvent.getLocalCertificates()).thenReturn(mockLocalCertificate);
        when(mockHandshakeCompleteEvent.getPeerCertificates()).thenReturn(mockPeerCertificates);
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ((HandshakeCompletedListener) invocationOnMock.getArguments()[0]).handshakeCompleted(mockHandshakeCompleteEvent);
                return null;
            }
        }).when(mockSslSocket).addHandshakeCompletedListener(any(HandshakeCompletedListener.class));
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSslSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        verify(mockSslSocket, times(1)).addHandshakeCompletedListener(httpServerConnection);
        assertThat(httpServerConnection.getLocalCertificateChain(), is(mockLocalCertificate));
        assertThat(httpServerConnection.getPeerCertificateChain(), is(mockPeerCertificates));
    }


    @Test
    public void inputStreamIsWrappedWithBufferedInputStream() throws Exception
    {
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        assertThat(httpServerConnection.getInputStream(), IsInstanceOf.instanceOf(BufferedInputStream.class));
    }

    @Test(expected = IllegalStateException.class)
    public void createHttpServerConnectionWithSocketAndFailForLocalCertificates() throws Exception
    {
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        httpServerConnection.getLocalCertificateChain();
    }

    @Test(expected = IllegalStateException.class)
    public void createHttpServerConnectionWithSocketAndFailForPeerCertificates() throws Exception
    {
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        httpServerConnection.getPeerCertificateChain();
    }

    @Test(expected = IllegalStateException.class)
    public void createHttpServerConnectionWithSocketAndFailForHandshakeLatch() throws Exception
    {
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        httpServerConnection.getSslSocketHandshakeCompleteLatch();
    }

    @Test
    public void resetConnectionReadNextRequest() throws Exception
    {
        configureValidRequestForSocketInputStream();
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        assertThat(httpServerConnection.getUrlWithoutRequestParams(), is("/service/order"));
        httpServerConnection.reset();
        assertThat(httpServerConnection.getUrlWithoutRequestParams(), is("/"));
    }

    private void configureValidRequestForSocketInputStream() throws IOException
    {
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(String.format("GET %s HTTP/1.1\n\nGET %s HTTP/1.1\n", "/service/order?param1=value1&param2=value2", "/?param1=value1&param2=value2").getBytes()));
    }

    @Test
    public void getRemoteSocketAddressWithNullSocketAddress() throws Exception
    {
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        when(mockSocket.getRemoteSocketAddress()).thenReturn(null);
        assertThat(httpServerConnection.getRemoteClientAddress(), IsNull.nullValue());
    }

    @Test
    public void getRemoteSocketAddress() throws Exception
    {
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        when(mockSocket.getRemoteSocketAddress()).thenReturn(new InetSocketAddress("host_abc", 1000));
        assertThat(httpServerConnection.getRemoteClientAddress(), is("host_abc:1000"));
    }

    @Test
    public void getUrlWithoutRequestParams() throws Exception
    {
        testUrlWithoutParams("/service/order?param1=value1&param2=value2", "/service/order");
        testUrlWithoutParams("/service/order", "/service/order");
        testUrlWithoutParams("/service?param1=value1&param2=value2", "/service");
        testUrlWithoutParams("/?param1=value1&param2=value2", "/");
        testUrlWithoutParams("/", "/");
    }

    @Test(expected = NullPointerException.class)
    public void writeResponseWithNullParams() throws Exception
    {
        ByteArrayOutputStream responseContent = new ByteArrayOutputStream();
        HttpServerConnection httpServerConnection = createHttpServerConnectionForResponseTest(responseContent);
        httpServerConnection.writeResponse(new HttpResponse(), null);
    }

    @Test
    public void writeResponseWithEmptyParams() throws Exception
    {
        ByteArrayOutputStream responseContent = new ByteArrayOutputStream();
        HttpServerConnection httpServerConnection = createHttpServerConnectionForResponseTest(responseContent);
        httpServerConnection.writeResponse(new HttpResponse(),new HashMap<String,String>());
        String response = new String(responseContent.toByteArray());
        assertThat(response.startsWith("HTTP/1.1 200 OK"), is(true));
        assertThat(response.contains("Connection: close"), is(true));
    }

    @Test
    public void writeResponseWithParams() throws Exception
    {
        ByteArrayOutputStream responseContent = new ByteArrayOutputStream();
        HttpServerConnection httpServerConnection = createHttpServerConnectionForResponseTest(responseContent);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("header1","value1");
        headers.put("header2","value2");
        httpServerConnection.writeResponse(new HttpResponse(), headers);
        String response = new String(responseContent.toByteArray());
        assertThat(response.startsWith("HTTP/1.1 200 OK"), is(true));
        assertThat(response.contains("header1: value1"), is(true));
        assertThat(response.contains("header2: value2"), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void writeFailureResponseWithNullParams() throws Exception
    {
        ByteArrayOutputStream responseContent = new ByteArrayOutputStream();
        HttpServerConnection httpServerConnection = createHttpServerConnectionForResponseTest(responseContent);
        httpServerConnection.writeFailureResponse(400, "failureMessage",null);
    }

    @Test
    public void writeFailureResponseWithEmptyParams() throws Exception
    {
        ByteArrayOutputStream responseContent = new ByteArrayOutputStream();
        HttpServerConnection httpServerConnection = createHttpServerConnectionForResponseTest(responseContent);
        httpServerConnection.writeFailureResponse(500, "failureMessage", new HashMap<String,String>());
        String response = new String(responseContent.toByteArray());
        assertThat(response.startsWith("HTTP/1.1 500"), is(true));
        assertThat(response.contains("Connection: close"), is(true));
        assertThat(response.endsWith("failureMessage"), is(true));
    }

    @Test
    public void writeFailureResponseWithParams() throws Exception
    {
        ByteArrayOutputStream responseContent = new ByteArrayOutputStream();
        HttpServerConnection httpServerConnection = createHttpServerConnectionForResponseTest(responseContent);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("header1","value1");
        headers.put("header2","value2");
        httpServerConnection.writeFailureResponse(429, "failureMessage", headers);
        String response = new String(responseContent.toByteArray());
        assertThat(response.startsWith("HTTP/1.1 429"), is(true));
        assertThat(response.contains("header1: value1"), is(true));
        assertThat(response.contains("header2: value2"), is(true));
        assertThat(response.endsWith("failureMessage"), is(true));
    }

    /**
     * Tests the correct propagation of {@link HttpConnector} properties to the {@link HttpServerConnection} (MULE-6884).
     * Unfortunately we can't use a mocked HTTP connector and a mocked socket for this test, so we need to use real ones.
     */
    @Test
    public void createHttpServerConnectionWithHttpConnectorProperties() throws Exception
    {
        // Build http connector and initialise it.
        HttpConnector httpConnector = new HttpConnector(muleContext);
        httpConnector.setSendTcpNoDelay(SEND_TCP_NO_DELAY);
        httpConnector.setKeepAlive(KEEP_ALIVE);
        httpConnector.setServerSoTimeout(SERVER_SO_TIMEOUT);
        httpConnector.initialise();

        ServerSocket serverSocket = null;
        Socket clientServerSocket = null;
        Socket serverClientSocket = null;
        try
        {
            // Establish server and client connections.
            serverSocket = httpConnector.getServerSocketFactory().createServerSocket(port1.getNumber(), TcpConnector.DEFAULT_BACKLOG, true);
            clientServerSocket = new Socket("localhost", port1.getNumber());
            serverClientSocket = serverSocket.accept();

            // Build HTTP server connection.
            HttpServerConnection conn = new HttpServerConnection(serverClientSocket, muleContext.getConfiguration().getDefaultEncoding(), httpConnector);

            // Assert that properties were propagated correctly from the connector.
            assertEquals(SEND_TCP_NO_DELAY, conn.isSocketTcpNoDelay());
            assertEquals(KEEP_ALIVE, conn.isSocketKeepAlive());
            assertEquals(SERVER_SO_TIMEOUT, conn.getSocketTimeout());
        }
        finally
        {
            // Close connections.
            if (clientServerSocket != null)
            {
                clientServerSocket.close();
            }
            if (serverClientSocket != null)
            {
                serverClientSocket.close();
            }
            if (serverSocket != null)
            {
                serverSocket.close();
            }
        }
    }

    @Test
    public void resetClosesRequestBody() throws Exception
    {
        configureValidRequestForSocketInputStream();
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector)
        {
            @Override
            protected HttpRequest createHttpRequest() throws IOException
            {
                return mockHttpRequest;
            }
        };
        httpServerConnection.readRequest();
        httpServerConnection.reset();
        verify(mockHttpRequest.getBody(), times(1)).close();
    }

    private HttpServerConnection createHttpServerConnectionForResponseTest(ByteArrayOutputStream responseContent) throws IOException
    {
        configureValidRequestForSocketInputStream();
        when(mockSocket.getOutputStream()).thenReturn(responseContent);
        return new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
    }

    private void testUrlWithoutParams(String requestUrl, String expectedUrlWithoutParams) throws IOException
    {
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(String.format("GET %s HTTP/1.0\n", requestUrl).getBytes()));
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        String urlWithoutParams = httpServerConnection.getUrlWithoutRequestParams();
        assertThat(urlWithoutParams, is(expectedUrlWithoutParams));
    }

}

