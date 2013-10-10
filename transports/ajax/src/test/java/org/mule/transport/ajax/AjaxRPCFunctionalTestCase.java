/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ajax;

import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mortbay.cometd.client.BayeuxClient;
import org.mortbay.jetty.client.Address;
import org.mortbay.jetty.client.HttpClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AjaxRPCFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    public static final String TEST_JSON_MESSAGE = "{\"data\" : {\"value1\" : \"foo\", \"value2\" : \"bar\"}, \"replyTo\" : \"/response\"}";

    public static int SERVER_PORT = -1;

    private BayeuxClient client;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public AjaxRPCFunctionalTestCase(AbstractServiceAndFlowTestCase.ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {AbstractServiceAndFlowTestCase.ConfigVariant.SERVICE, "ajax-rpc-test.xml"},
            {AbstractServiceAndFlowTestCase.ConfigVariant.FLOW, "ajax-rpc-test-flow.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        // FIXME DZ: we don't use the inherited SERVER_PORT here because it's not set
        // at this point and we can't move super.doSetUp() above this
        SERVER_PORT = dynamicPort.getNumber();
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

    @Test
    public void testDispatchReceiveSimple() throws Exception
    {
        final Latch latch = new Latch();

        final AtomicReference<Object> data = new AtomicReference<Object>();
        client.addListener(new MessageListener()
        {
            @Override
            public void deliver(Client fromClient, Client toClient, Message message)
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
        latch.await(DEFAULT_TEST_TIMEOUT_SECS, TimeUnit.SECONDS);
        assertNotNull(data.get());
        assertEquals("{\"value1\":\"foo\",\"value2\":\"bar\"}", data.get());
    }
}
