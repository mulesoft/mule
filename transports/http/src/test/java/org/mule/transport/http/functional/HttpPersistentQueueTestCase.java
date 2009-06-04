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

import org.mule.api.MuleEventContext;
import org.mule.api.transport.MessageAdapter;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.transport.http.HttpConstants;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class HttpPersistentQueueTestCase extends FunctionalTestCase
{
    private CountDownLatch messageDidArrive = new CountDownLatch(1);

    @Override
    protected String getConfigResources()
    {
        return "http-persistent-queue.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("PersistentQueueAsync");
        assertNotNull(testComponent);
        testComponent.setEventCallback(new Callback(messageDidArrive));
    }

    public void testPersistentMessageDeliveryWithGet() throws Exception
    {
        GetMethod method = new GetMethod("http://localhost:63083/services/Echo?foo=bar");
        method.addRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        doTestPersistentMessageDelivery(method);
    }

    public void testPersistentMessageDeliveryWithPost() throws Exception
    {        
        PostMethod method = new PostMethod("http://localhost:63083/services/Echo");        
        method.addRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        method.addParameter(new NameValuePair("foo", "bar"));
        doTestPersistentMessageDelivery(method);
    }
    
    private void doTestPersistentMessageDelivery(HttpMethod httpMethod) throws Exception
    {
        HttpClient client = new HttpClient();
        int rc = client.executeMethod(httpMethod);
        
        assertEquals(HttpStatus.SC_OK, rc);
        assertTrue(messageDidArrive.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }
    
    private static class Callback implements EventCallback
    {
        private CountDownLatch messageDidArrive;
        
        public Callback(CountDownLatch latch)
        {
            super();
            messageDidArrive = latch;
        }
        
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            MessageAdapter adapter = context.getMessage().getAdapter();
            
            // make sure that the message adapter that's deserialized is the right one
            assertTrue(adapter instanceof DefaultMessageAdapter);

            assertEquals("true", adapter.getProperty(HttpConstants.HEADER_CONNECTION));
            assertEquals("true", adapter.getProperty(HttpConstants.HEADER_KEEP_ALIVE));
            
            messageDidArrive.countDown();            
        }
    }
    
}
