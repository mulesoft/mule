/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.util.concurrent.Latch;

import java.io.BufferedReader;
import java.util.StringTokenizer;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpOutboundTestCase extends AbstractMockHttpServerTestCase
{

    @ClassRule
    public static DynamicPort dynamicPort = new DynamicPort("port1");

    private Latch testLatch = new Latch();
    private String httpMethod;

    public HttpOutboundTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "http-outbound-config.xml";
    }

    @Override
    protected MockHttpServer getHttpServer(CountDownLatch latch)
    {
        return new SimpleHttpServer(dynamicPort.getNumber(), latch, testLatch);
    }

    @Test
    public void testOutboundDelete() throws Exception
    {
        sendHttpRequest("vm://doDelete", HttpConstants.METHOD_DELETE);
    }

    @Test
    public void testOutboundGet() throws Exception
    {
        sendHttpRequest("vm://doGet", HttpConstants.METHOD_GET);
    }

    @Test
    public void testOutboundHead() throws Exception
    {
        sendHttpRequest("vm://doHead", HttpConstants.METHOD_HEAD);
    }

    @Test
    public void testOutboundOptions() throws Exception
    {
        sendHttpRequest("vm://doOptions", HttpConstants.METHOD_OPTIONS);
    }

    @Test
    public void testOutboundPost() throws Exception
    {
        sendHttpRequest("vm://doPost", HttpConstants.METHOD_POST);
    }

    @Test
    public void testOutboundPut() throws Exception
    {
        sendHttpRequest("vm://doPut", HttpConstants.METHOD_PUT);
    }

    @Test
    public void testOutboundTrace() throws Exception
    {
        sendHttpRequest("vm://doTrace", HttpConstants.METHOD_TRACE);
    }

    private void sendHttpRequest(String endpoint, String expectedHttpMethod) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch(endpoint, TEST_MESSAGE, null);

        assertTrue(testLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(expectedHttpMethod, httpMethod);
    }

    private class SimpleHttpServer extends MockHttpServer
    {
        public SimpleHttpServer(int listenPort, CountDownLatch startupLatch, CountDownLatch testCompleteLatch)
        {
            super(listenPort, startupLatch, testCompleteLatch);
        }

        @Override
        protected void readHttpRequest(BufferedReader reader) throws Exception
        {
            // first line is the HTTP request
            String line = reader.readLine();
            httpMethod = new StringTokenizer(line).nextToken();
        }
    }
}
