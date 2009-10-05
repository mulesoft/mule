/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.components;

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.functional.MockHttpServer;

import java.io.BufferedReader;
import java.util.StringTokenizer;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class RestServiceComponentDeleteTestCase extends FunctionalTestCase
{
    private static final int LISTEN_PORT = 60205;
    
    private CountDownLatch serverStartLatch = new CountDownLatch(1);
    private CountDownLatch serverRequestCompleteLatch = new CountDownLatch(1);
    private boolean deleteRequestFound = false;
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // start a simple HTTP server that parses the request sent from Mule
        SimpleHttpServer httServer = new SimpleHttpServer(LISTEN_PORT, serverStartLatch,
            serverRequestCompleteLatch);
        new Thread(httServer).start();        
    }

    @Override
    protected String getConfigResources()
    {
        return "rest-service-component-delete-test.xml";
    }
    
    public void testRestServiceComponentDelete() throws Exception
    {
        // wait for the simple server thread started in doSetUp to come up
        assertTrue(serverStartLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));

        MuleClient client = new MuleClient();
        client.send("vm://fromTest", TEST_MESSAGE, null);
        
        assertTrue(serverRequestCompleteLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(deleteRequestFound);
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
            String requestLine = reader.readLine();
            String httpMethod = new StringTokenizer(requestLine).nextToken();
            
            deleteRequestFound = httpMethod.equals(HttpConstants.METHOD_DELETE);
        }
    }
}
