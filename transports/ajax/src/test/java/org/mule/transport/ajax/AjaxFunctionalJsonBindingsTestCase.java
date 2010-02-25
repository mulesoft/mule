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

import java.util.concurrent.atomic.AtomicReference;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.mortbay.cometd.client.BayeuxClient;
import org.mortbay.jetty.client.Address;
import org.mortbay.jetty.client.HttpClient;

public class AjaxFunctionalJsonBindingsTestCase extends FunctionalTestCase
{
    public static final int SERVER_PORT = 58080;

    private BayeuxClient client;

    @Override
    protected String getConfigResources()
    {
        return "ajax-embedded-functional-json-bindings-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        HttpClient http = new HttpClient();
        http.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);

        client = new BayeuxClient(http, new Address("localhost", SERVER_PORT), "/ajax/cometd");
        http.start();
        //need to start the client before you can add subscriptions
        client.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        //9 times out of 10 this throws a "ava.lang.IllegalStateException: Not running" exception, it can be ignored
        //client.stop();
    }

     public void testClientSubscribeWithJsonObjectResponse() throws Exception
    {

        final Latch latch = new Latch();

        final AtomicReference<Object> data = new AtomicReference<Object>();
        client.addListener(new MessageListener()
        {
            public void deliver(Client client, Client client1, Message message)
            {
                if (message.getData() != null)
                {
                    //This simulate what the browser would receive
                    data.set((message.toString()));
                    latch.release();
                }
            }
        });
        client.subscribe("/test1");

        MuleClient muleClient = new MuleClient();
        muleClient.dispatch("vm://in1", "Ross", null);
        latch.await(10, TimeUnit.SECONDS);

        assertNotNull(data.get());
        assertEquals("{\"data\":{\"name\":\"Ross\"},\"channel\":\"/test1\"}", data.get());
    }

    public void testClientPublishWithJsonObject() throws Exception
    {
        client.publish("/test2", "{\"name\":\"Ross\"}", null);
        MuleClient muleClient = new MuleClient();
        MuleMessage msg = muleClient.request("vm://in2", 5000L);

        assertNotNull(msg);
        assertEquals("Received: DummyJsonBean{name='Ross'}", msg.getPayloadAsString());
    }
}