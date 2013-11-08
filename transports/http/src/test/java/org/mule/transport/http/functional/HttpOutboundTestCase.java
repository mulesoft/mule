/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpRequest;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpOutboundTestCase extends AbstractMockHttpServerTestCase
{

    @ClassRule
    public static DynamicPort dynamicPort = new DynamicPort("port1");

    private Latch testLatch = new Latch();
    private String httpMethod;
    private String body;

    public HttpOutboundTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-outbound-config-service.xml"},
            {ConfigVariant.FLOW, "http-outbound-config-flow.xml"}
        });
    }

    @Override
    protected MockHttpServer getHttpServer()
    {
        return new SimpleHttpServer(dynamicPort.getNumber());
    }

    @Test
    public void testOutboundDelete() throws Exception
    {
        sendHttpRequest("vm://doDelete", HttpConstants.METHOD_DELETE);
        assertEmptyRequestBody();
    }

    @Test
    public void testOutboundGet() throws Exception
    {
        sendHttpRequest("vm://doGet", HttpConstants.METHOD_GET);
        assertEmptyRequestBody();
    }

    @Test
    public void testOutboundHead() throws Exception
    {
        sendHttpRequest("vm://doHead", HttpConstants.METHOD_HEAD);
        assertEmptyRequestBody();
    }

    @Test
    public void testOutboundOptions() throws Exception
    {
        sendHttpRequest("vm://doOptions", HttpConstants.METHOD_OPTIONS);
        assertEmptyRequestBody();
    }

    @Test
    public void testOutboundPost() throws Exception
    {
        sendHttpRequest("vm://doPost", HttpConstants.METHOD_POST);
        assertPayloadInRequestBody();
    }

    @Test
    public void testOutboundPut() throws Exception
    {
        sendHttpRequest("vm://doPut", HttpConstants.METHOD_PUT);
        assertPayloadInRequestBody();
    }

    @Test
    public void testOutboundTrace() throws Exception
    {
        sendHttpRequest("vm://doTrace", HttpConstants.METHOD_TRACE);
        assertEmptyRequestBody();
    }

    @Test
    public void testOutboundPatch() throws Exception
    {
        sendHttpRequest("vm://doPatch", HttpConstants.METHOD_PATCH);
        assertPayloadInRequestBody();
    }

    private void sendHttpRequest(String endpoint, String expectedHttpMethod) throws Exception
    {
        muleContext.getClient().dispatch(endpoint, TEST_MESSAGE, null);

        assertTrue(testLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(expectedHttpMethod, httpMethod);
    }

    private void assertPayloadInRequestBody()
    {
        assertEquals(TEST_MESSAGE, body);
    }

    private void assertEmptyRequestBody()
    {
        assertNull(body);
    }

    private class SimpleHttpServer extends SingleRequestMockHttpServer
    {

        public SimpleHttpServer(int listenPort)
        {
            super(listenPort, muleContext.getConfiguration().getDefaultEncoding());
        }

        @Override
        protected void processSingleRequest(HttpRequest httpRequest) throws Exception
        {
            httpMethod = httpRequest.getRequestLine().getMethod();
            body = httpRequest.getBodyString();
            testLatch.countDown();
        }
    }
}
