/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ajax;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
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

public class AjaxFunctionalTestCase extends FunctionalTestCase
{
    public static final int SERVER_PORT = 58080;
    
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
        httpClient = new HttpClient();
        httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        httpClient.start();

        bayeuxClient = new BayeuxClient(httpClient, new Address("localhost", SERVER_PORT), "/ajax/cometd");
        // need to start the client before you can add subscriptions
        bayeuxClient.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (httpClient.isRunning())
        {
            httpClient.stop();
        }
    }

    public void testClientSubscribeWithString() throws Exception
    {
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

        MuleClient muleClient = new MuleClient();
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
        bayeuxClient.publish("/test2", "Ross", null);
        MuleClient muleClient = new MuleClient();
        MuleMessage msg = muleClient.request("vm://in2", 5000L);

        assertNotNull(msg);
        assertEquals("Ross Received", msg.getPayloadAsString());
    }
}