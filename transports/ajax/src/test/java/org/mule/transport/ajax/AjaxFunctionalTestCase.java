/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ajax;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.util.concurrent.Latch;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.mortbay.cometd.client.BayeuxClient;
import org.mortbay.jetty.client.Address;
import org.mortbay.jetty.client.HttpClient;

public class AjaxFunctionalTestCase extends DynamicPortTestCase
{
    public static int SERVER_PORT = -1;
    
    private HttpClient httpClient;
    private BayeuxClient bayeuxClient;

    @Override
    protected String getConfigResources()
    {
        return "ajax-embedded-functional-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        SERVER_PORT = getPorts().get(0);
        httpClient = new HttpClient();
        httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        httpClient.start();

        bayeuxClient = new BayeuxClient(httpClient, new Address("localhost", SERVER_PORT), "/ajax/cometd");
        // need to start the client before you can add subscriptions
        bayeuxClient.start();
        
        assertTrue("httpClient is not running", httpClient.isRunning());
        assertTrue("bayeuxClient is not running", bayeuxClient.isRunning());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (httpClient.isRunning())
        {
            httpClient.stop();
        }

        try
        {
            /*
             * always try to stop the client as I think there is a timing issue of it
             * staying up between tests and even if it thinks it's running, calling
             * stop sometimes throws an exception
             */
            bayeuxClient.stop();
        }
        catch (Exception e)
        {
            // dont do anything
        }
    }

    public void testClientSubscribeWithString() throws Exception
    {
        /*
         * Give mule and the clients time to warm up; we get an intermittent failure,
         * see if this helps
         */
        Thread.sleep(5000); 
        final Latch latch = new Latch();

        final AtomicReference<Object> data = new AtomicReference<Object>();
        bayeuxClient.addListener(new MessageListener()
        {
            public void deliver(Client fromClient, Client toClient, Message message)
            {

                if (message.getData() != null)
                {
                    // This simulates what the browser would receive
                    data.set(message.toString());
                    latch.release();
                }
            }
        });
        bayeuxClient.subscribe("/test1");

        MuleClient muleClient = new MuleClient(muleContext);
        muleClient.dispatch("vm://in1", "Ross", null);

        latch.await(10, TimeUnit.SECONDS);
        assertNotNull(data.get());
        
        // parse the result string into java objects.  different jvms return it in different order, so we can't do a straight string comparison 
        ObjectMapper mapper = new ObjectMapper();
        Map result  = mapper.readValue((String) data.get(), Map.class);
        assertEquals("/test1", result.get("channel"));
        assertEquals("Ross Received", result.get("data"));
    }

    public void testClientPublishWithString() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);

        bayeuxClient.publish("/test2", "Ross", null);
        MuleMessage msg = muleClient.request("vm://in2", RECEIVE_TIMEOUT);

        assertNotNull(msg);
        assertEquals("Ross Received", msg.getPayloadAsString());
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}
