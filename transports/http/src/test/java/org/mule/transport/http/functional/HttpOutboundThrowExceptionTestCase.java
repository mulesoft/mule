/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpRequest;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpOutboundThrowExceptionTestCase extends AbstractMockHttpServerTestCase
{

    @ClassRule
    public static DynamicPort inboundPort = new DynamicPort("portIn");

    @ClassRule
    public static DynamicPort outboundPort = new DynamicPort("portOut");

    private Latch testLatch = new Latch();

    public HttpOutboundThrowExceptionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "http-outbound-throw-exception-config.xml"}
        });
    }

    @Override
    protected MockHttpServer getHttpServer()
    {
        return new SimpleHttpServer(outboundPort.getNumber());
    }

    @Test
    public void errorStatusPropagation() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
        MuleMessage result = client.send("errorPropagationEndpoint", TEST_MESSAGE, props);
        assertThat((String) result.getInboundProperty("http.status"), is("400"));
    }

    @Test
    public void errorStatusThrowException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
        MuleMessage result = client.send("exceptionOnErrorStatusEndpoint", TEST_MESSAGE, props);
        assertThat((String) result.getInboundProperty("http.status"), is("500"));
    }

    private class SimpleHttpServer extends SingleRequestMockHttpServer
    {

        private static final String HTTP_STATUS_LINE_BAD_REQUEST = "HTTP/1.1 400 Bad Request\n";

        public SimpleHttpServer(int listenPort)
        {
            super(listenPort, muleContext.getConfiguration().getDefaultEncoding(), HTTP_STATUS_LINE_BAD_REQUEST);
        }

        @Override
        protected void processSingleRequest(HttpRequest httpRequest) throws Exception
        {
            testLatch.countDown();
        }
    }
}
