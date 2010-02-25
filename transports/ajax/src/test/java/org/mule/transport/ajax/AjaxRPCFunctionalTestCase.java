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

public class AjaxRPCFunctionalTestCase extends FunctionalTestCase
{
    public static final String TEST_JSON_MESSAGE = "{\"data\" : {\"value1\" : \"foo\", \"value2\" : \"bar\"}, \"replyTo\" : \"/response\"}";

    public static final int SERVER_PORT = 58080;

    private BayeuxClient client;

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
        client.stop();
    }

    @Override
    protected String getConfigResources()
    {
        return "ajax-rpc-test.xml";
    }

    public void testDispatchReceiveSimple() throws Exception
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
                    data.set((message.getData()));
                    latch.release();
                }
            }
        });
        //The '/response' channel is set on the request message
        client.subscribe("/response");
        //Simulates dispatching from the browser
        client.publish("/request", TEST_JSON_MESSAGE, null);
        latch.await(10, TimeUnit.SECONDS);

        assertNotNull(data.get());
        assertEquals("{\"value1\":\"foo\",\"value2\":\"bar\"}", data.get());
    }
}