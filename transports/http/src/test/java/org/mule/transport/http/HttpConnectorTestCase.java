/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.tck.MuleTestUtils.TestCallback;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.tcp.TcpConnector;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HttpConnectorTestCase extends AbstractConnectorTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpMessageReceiver mockServiceOrderReceiverPort5555;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpMessageReceiver mockServiceReceiverPort5555;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpMessageReceiver mockServiceOrderReceiverPort7777;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpMessageReceiver mockServiceReceiverPort7777;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpMessageReceiver mockServiceReceiverAnotherHost;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpMessageReceiver mockReceiverPort5555;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpRequest mockHttpRequest;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Socket mockSocket;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RequestLine mockRequestLine;

    @Override
    public Connector createConnector() throws Exception
    {
        HttpConnector c = new HttpConnector(muleContext);
        c.setName("HttpConnector");
        return c;
    }

    public String getTestEndpointURI()
    {
        return "http://localhost:60127";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    @Test
    public void testValidListener() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
                getTestEndpointURI());

        getConnector().registerListener(endpoint, getSensingNullMessageProcessor(), service);
    }

    @Test
    public void testProperties() throws Exception
    {
        HttpConnector c = (HttpConnector) getConnector();

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());

        int maxDispatchers = c.getMaxTotalDispatchers();
        HttpConnectionManagerParams params = c.getClientConnectionManager().getParams();
        assertEquals(maxDispatchers, params.getDefaultMaxConnectionsPerHost());
        assertEquals(maxDispatchers, params.getMaxTotalConnections());

        // all kinds of timeouts are now being tested in TcpConnectorTestCase
    }

    @Test
    public void findReceiverByStem() throws Exception
    {
        Map<Object, MessageReceiver> receiversMap = createTestReceivers();
        assertThat((HttpMessageReceiver) HttpConnector.findReceiverByStem(receiversMap, "http://somehost:5555/"), Is.is(mockReceiverPort5555));
        assertThat((HttpMessageReceiver) HttpConnector.findReceiverByStem(receiversMap, "http://somehost:5555/service"), Is.is(mockServiceReceiverPort5555));
        assertThat((HttpMessageReceiver) HttpConnector.findReceiverByStem(receiversMap, "http://somehost:5555/service/order"), Is.is(mockServiceOrderReceiverPort5555));
        assertThat((HttpMessageReceiver) HttpConnector.findReceiverByStem(receiversMap, "http://somehost:7777/service/order"), Is.is(mockServiceOrderReceiverPort7777));
        assertThat((HttpMessageReceiver) HttpConnector.findReceiverByStem(receiversMap, "http://somehost:7777/service"), Is.is(mockServiceReceiverPort7777));
        assertThat((HttpMessageReceiver) HttpConnector.findReceiverByStem(receiversMap, "http://anotherhost:5555/"), Is.is(mockServiceReceiverAnotherHost));
    }

    private Map<Object, MessageReceiver> createTestReceivers()
    {
        Map<Object, MessageReceiver> receiversMap = new HashMap<Object, MessageReceiver>();
        receiversMap.put("http://somehost:5555/service/order", mockServiceOrderReceiverPort5555);
        receiversMap.put("http://somehost:5555/service", mockServiceReceiverPort5555);
        receiversMap.put("http://somehost:5555/", mockReceiverPort5555);
        receiversMap.put("http://somehost:7777/service/order", mockServiceOrderReceiverPort7777);
        receiversMap.put("http://somehost:7777/service", mockServiceReceiverPort7777);
        receiversMap.put("http://anotherhost:5555/", mockServiceReceiverAnotherHost);
        return receiversMap;
    }

    @Test
    public void lookupReceiverThatDoesNotExistsInThatPort() throws Exception
    {
        testLookupReceiver("somehost", 8888, "/management", null);
    }

    @Test
    public void lookupReceiverThatDoesNotExistsInThatHost() throws Exception
    {
        testLookupReceiver("nonexistenthost", 5555, "/service", null);
    }

    @Test
    public void lookupReceiverThatContainsPath() throws Exception
    {
        when(mockServiceReceiverPort5555.getEndpointURI().getPort()).thenReturn(5555);
        when(mockServiceReceiverPort5555.getEndpointURI().getHost()).thenReturn("somehost");
        testLookupReceiver("somehost", 5555, "/service/product", mockServiceReceiverPort5555);
    }

    @Test
    public void lookupReceiverThatExistsWithExactSamePath() throws Exception
    {
        when(mockServiceReceiverPort5555.getEndpointURI().getPort()).thenReturn(5555);
        when(mockServiceReceiverPort5555.getEndpointURI().getHost()).thenReturn("somehost");
        testLookupReceiver("somehost", 5555, "/service/order?param1=value1", mockServiceOrderReceiverPort5555);
    }

    @Test
    public void lookupReceiverByRequestLineThatDoesNotExistsInThatPort() throws Exception
    {
        testLookupReceiverByRequestLine("somehost", 8888, "/management", null);
    }

    @Test
    public void lookupReceiverByRequestLineThatDoesNotExistsInThatHost() throws Exception
    {
        testLookupReceiverByRequestLine("nonexistenthost", 5555, "/service", null);
    }

    @Test
    public void lookupReceiverByRequestLineThatContainsPath() throws Exception
    {
        when(mockServiceReceiverPort5555.getEndpointURI().getPort()).thenReturn(5555);
        when(mockServiceReceiverPort5555.getEndpointURI().getHost()).thenReturn("somehost");
        testLookupReceiverByRequestLine("somehost", 5555, "/service/product", mockServiceReceiverPort5555);
    }

    @Test
    public void lookupReceiverByRequestLineThatExistsWithExactSamePath() throws Exception
    {
        when(mockServiceReceiverPort5555.getEndpointURI().getPort()).thenReturn(5555);
        when(mockServiceReceiverPort5555.getEndpointURI().getHost()).thenReturn("somehost");
        testLookupReceiverByRequestLine("somehost", 5555, "/service/order?param1=value1", mockServiceOrderReceiverPort5555);
    }


    private void testLookupReceiver(String host, int port, String path, HttpMessageReceiver expectedMessageReceiver)
    {
        HttpConnector httpConnector = (HttpConnector) getConnector();
        httpConnector.getReceivers().putAll(createTestReceivers());
        when(mockHttpRequest.getRequestLine()).thenReturn(mockRequestLine);
        when(mockRequestLine.getUrlWithoutParams()).thenReturn(path);
        when(mockSocket.getLocalSocketAddress()).thenReturn(new InetSocketAddress(host, port));
        Assert.assertThat(httpConnector.lookupReceiver(mockSocket, mockHttpRequest), Is.is(expectedMessageReceiver));
    }

    private void testLookupReceiverByRequestLine(String host, int port, String path, HttpMessageReceiver expectedMessageReceiver) throws NoReceiverForEndpointException
    {
        HttpConnector httpConnector = (HttpConnector) getConnector();
        httpConnector.getReceivers().putAll(createTestReceivers());
        when(mockRequestLine.getUrlWithoutParams()).thenReturn(path);
        when(mockSocket.getLocalSocketAddress()).thenReturn(new InetSocketAddress(host, port));
        if (expectedMessageReceiver != null)
        {
            Assert.assertThat(httpConnector.lookupReceiver(mockSocket, mockRequestLine), Is.is(expectedMessageReceiver));
        }
        else
        {
            try
            {
                httpConnector.lookupReceiver(mockSocket, mockRequestLine);
                fail("exception should be thrown");
            }
            catch (NoReceiverForEndpointException e)
            {
            }
        }
    }
    
    @Test
    public void tcpNoDelayDefault() throws Exception
    {
        assertFalse(((TcpConnector) getConnector()).isSendTcpNoDelay());
    }
    
    @Test
    public void tcpNoDelayDefaultSystemPropertyTrue() throws Exception
    {
        testWithSystemProperty(TcpConnector.SEND_TCP_NO_DELAY_SYSTEM_PROPERTY, "true", new TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                assertTrue(((HttpConnector) createConnector()).isSendTcpNoDelay());

            }
        });
    }

    @Test
    public void tcpNoDelayDefaultSystemPropertyFalse() throws Exception
    {
        testWithSystemProperty(TcpConnector.SEND_TCP_NO_DELAY_SYSTEM_PROPERTY, "false", new TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                assertFalse(((HttpConnector) createConnector()).isSendTcpNoDelay());

            }
        });
    }
    
    @Test
    public void tcpNoDelayHttpClientConnectionManagerConfiguration() throws Exception
    {
        HttpConnector httpConnector = (HttpConnector) createConnector();
        httpConnector.initialise();
        assertFalse(httpConnector.clientConnectionManager.getParams().getTcpNoDelay());

        httpConnector = (HttpConnector) createConnector();
        httpConnector.setSendTcpNoDelay(true);
        httpConnector.initialise();
        assertTrue(httpConnector.clientConnectionManager.getParams().getTcpNoDelay());
    }
    
    @Test
    public void httpClientDisableStaleConnectionDefault() throws Exception
    {
        assertHttpClientStaleConnectionCheck(getInitialisedHttpConnector(), true);
    }

    @Test
    public void httpClientDisableStaleConnectionSystemProperty() throws Exception
    {
        testWithSystemProperty(HttpConnector.DISABLE_STALE_CONNECTION_CHECK_SYSTEM_PROPERTY, "true",
            new TestCallback()
            {
                @Override
                public void run() throws Exception
                {
                    assertHttpClientStaleConnectionCheck(getInitialisedHttpConnector(), false);
                }
            });
    }

    protected HttpConnector getInitialisedHttpConnector() throws Exception, InitialisationException
    {
        HttpConnector httpConnector = (HttpConnector) createConnector();
        httpConnector.initialise();
        return httpConnector;
    }

    protected void assertHttpClientStaleConnectionCheck(HttpConnector connector, boolean enabled)
        throws Exception, InitialisationException
    {
        assertEquals(enabled, connector.clientConnectionManager.getParams().isStaleCheckingEnabled());
    }

    public void singleDispatcherPerEndpointSyetemProperty() throws Exception
    {
        testWithSystemProperty(HttpConnector.SINGLE_DISPATCHER_PER_ENDPOINT_SYSTEM_PROPERTY, "true",
            new TestCallback()
            {

                @Override
                public void run() throws Exception
                {
                    HttpConnector httpConnector = (HttpConnector) createConnector();
                    httpConnector.initialise();

                    OutboundEndpoint endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
                        "http://localhost:8080");
                    httpConnector.createDispatcherMessageProcessor(endpoint);

                    assertNotNull(httpConnector.borrowDispatcher(endpoint));
                    assertEquals(httpConnector.borrowDispatcher(endpoint),
                        httpConnector.borrowDispatcher(endpoint));
                    assertEquals(0, httpConnector.getDispatchers().getNumIdle());
                }
            });
    }

}
