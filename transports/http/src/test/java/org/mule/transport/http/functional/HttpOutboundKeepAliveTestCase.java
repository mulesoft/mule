/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.Header;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpOutboundKeepAliveTestCase extends AbstractMockHttpServerTestCase
{

    private static final String KEEP_ALIVE_ONE_WAY_PATH = "vm://keepAliveOneWay";
    private static final String KEEP_ALIVE_REQUEST_RESPONSE_PATH = "vm://keepAliveRequestResponse";
    private static final String NO_KEEP_ALIVE_ONE_WAY_PATH = "vm://noKeepAliveOneWay";
    private static final String NO_KEEP_ALIVE_REQUEST_RESPONSE_PATH = "vm://noKeepAliveRequestResponse";
    private static final String DEFAULT_KEEP_ALIVE_ONE_WAY_PATH = "vm://defaultKeepAliveOneWay";
    private static final String DEFAULT_KEEP_ALIVE_REQUEST_RESPONSE_PATH = "vm://defaultKeepAliveRequestResponse";

    private static final String CONNECTION_CLOSE_VALUE = "close";

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    private volatile String connectionHeader;
    private volatile int requestCount;

    private Prober prober = new PollingProber(2000, 100);

    public HttpOutboundKeepAliveTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "http-outbound-keep-alive.xml"}});
    }

    @Override
    protected MockHttpServer getHttpServer()
    {
        return new KeepAliveHTTPServer(httpPort.getNumber());
    }

    @Test
    public void closesConnectionWhenOneWayEndpointHasNoKeepAlive() throws MuleException
    {
        assertConnectionClosed(NO_KEEP_ALIVE_ONE_WAY_PATH);
    }

    @Test
    public void closesConnectionWhenRequestResponseEndpointHasNoKeepAlive() throws MuleException
    {
        assertConnectionClosed(NO_KEEP_ALIVE_ONE_WAY_PATH);
    }

    @Test
    public void reusesConnectionWhenOneWayEndpointHasKeepAlive() throws MuleException
    {
        assertKeepAlive(KEEP_ALIVE_ONE_WAY_PATH);
    }

    @Test
    @Ignore("MULE-6926: Flaky test")
    public void reusesConnectionWhenRequestResponseEndpointHasKeepAlive() throws MuleException
    {
        assertKeepAlive(KEEP_ALIVE_REQUEST_RESPONSE_PATH);
    }


    @Test
    public void reusesConnectionWhenOneWayEndpointHasDefaultKeepAlive() throws MuleException
    {
        assertKeepAlive(DEFAULT_KEEP_ALIVE_ONE_WAY_PATH);
    }

    @Test
    public void reusesConnectionWhenRequestResponseEndpointHasDefaultKeepAlive() throws MuleException
    {
        assertKeepAlive(DEFAULT_KEEP_ALIVE_REQUEST_RESPONSE_PATH);
    }

    private void assertKeepAlive(String endpoint) throws MuleException
    {
        muleContext.getClient().dispatch(endpoint, TEST_MESSAGE, null);
        assertRequestCount(1);
        muleContext.getClient().dispatch(endpoint, TEST_MESSAGE, null);
        assertRequestCount(2);
    }

    private void assertConnectionClosed(String endpoint) throws MuleException
    {
        muleContext.getClient().dispatch(NO_KEEP_ALIVE_REQUEST_RESPONSE_PATH, TEST_MESSAGE, null);
        assertRequestCount(1);
        assertEquals(CONNECTION_CLOSE_VALUE, connectionHeader);
    }

    private void assertRequestCount(final int expectedRequestCount)
    {
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return requestCount == expectedRequestCount;
            }

            @Override
            public String describeFailure()
            {
                return String.format("Expected %d requests but received %d.", expectedRequestCount, requestCount);
            }
        });
    }


    private class KeepAliveHTTPServer extends MockHttpServer
    {

        private static final int MAX_REQUESTS = 2;

        public KeepAliveHTTPServer(int port)
        {
            super(port);
        }

        @Override
        protected void processRequests(InputStream in, OutputStream out) throws IOException
        {
            boolean closeConnection = false;

            while (requestCount < MAX_REQUESTS && !closeConnection)
            {
                HttpRequest request = parseRequest(in, muleContext.getConfiguration().getDefaultEncoding());
                Header connHeader = request.getFirstHeader(HttpConstants.HEADER_CONNECTION);

                connectionHeader = (connHeader == null) ? null : connHeader.getValue();
                requestCount++;

                closeConnection = CONNECTION_CLOSE_VALUE.equals(connectionHeader);

                StringBuilder response = new StringBuilder(HTTP_STATUS_LINE_OK);

                if (closeConnection)
                {
                    response.append(String.format("%s: %s\n", HttpConstants.HEADER_CONNECTION, CONNECTION_CLOSE_VALUE));
                }
                response.append("Content-Length: 4\n\nTEST");

                out.write(response.toString().getBytes());
            }
        }
    }
}
