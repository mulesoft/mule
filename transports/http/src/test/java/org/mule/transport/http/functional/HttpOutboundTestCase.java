/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

import java.io.BufferedReader;
import java.util.StringTokenizer;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class HttpOutboundTestCase extends FunctionalTestCase
{
    private static final int LISTEN_PORT = 60215;
    private CountDownLatch httpServerLatch = new CountDownLatch(1);
    private CountDownLatch testLatch = new CountDownLatch(1);
    private String httpMethod = null;
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        new Thread(new SimpleHttpServer(LISTEN_PORT, httpServerLatch, testLatch)).start();
    }

    protected String getConfigResources()
    {
        return "http-outbound-config.xml";
    }
    
    public void testOutboundDelete() throws Exception
    {
        sendHttpRequest("vm://doDelete", HttpConstants.METHOD_DELETE);
    }

    public void testOutboundGet() throws Exception
    {
        sendHttpRequest("vm://doGet", HttpConstants.METHOD_GET);
    }

    public void testOutboutHead() throws Exception
    {
        sendHttpRequest("vm://doHead", HttpConstants.METHOD_HEAD);
    }

    public void testOutboundOptions() throws Exception
    {
        sendHttpRequest("vm://doOptions", HttpConstants.METHOD_OPTIONS);
    }

    public void testOutboundPost() throws Exception
    {
        sendHttpRequest("vm://doPost", HttpConstants.METHOD_POST);
    }

    public void testOutboundPut() throws Exception
    {
        sendHttpRequest("vm://doPut", HttpConstants.METHOD_PUT);
    }

    public void testOutboundTrace() throws Exception
    {
        sendHttpRequest("vm://doTrace", HttpConstants.METHOD_TRACE);
    }

    private void sendHttpRequest(String endpoint, String expectedHttpMethod) throws Exception
    {
        // wait for the simple server thread started in doSetUp to come up
        assertTrue(httpServerLatch.await(LOCK_TIMEOUT, TimeUnit.MILLISECONDS));
        
        MuleClient client = new MuleClient();
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


