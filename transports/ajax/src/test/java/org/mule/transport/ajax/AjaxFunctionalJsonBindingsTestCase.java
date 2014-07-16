/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mortbay.cometd.client.BayeuxClient;
import org.mortbay.jetty.client.Address;
import org.mortbay.jetty.client.HttpClient;

public class AjaxFunctionalJsonBindingsTestCase extends AbstractServiceAndFlowTestCase
{
    public AjaxFunctionalJsonBindingsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    public static int SERVER_PORT = -1;

    private BayeuxClient client;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "ajax-embedded-functional-json-bindings-test-service.xml"},
            {ConfigVariant.FLOW, "ajax-embedded-functional-json-bindings-test-flow.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
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
        //client.stop();
        // TODO DZ: it seems like you would want to do this, maybe causing port locking issues?
        try
        {
            client.stop();
        }
        catch (IllegalStateException e)
        {
            logger.info("caught an IllegalStateException during tearDown", e);
        }
        catch(Exception e1)
        {
            fail("unexpected exception during tearDown :" + e1.getMessage());
        }
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testClientSubscribeWithJsonObjectResponse() throws Exception
    {
        final Latch latch = new Latch();

        final AtomicReference<String> data = new AtomicReference<String>();
        client.addListener(new MessageListener()
        {
            @Override
            public void deliver(Client fromClient, Client toClient, Message message)
            {
                if (message.getData() != null)
                {
                    // This simulate what the browser would receive
                    data.set(message.toString());
                    latch.release();
                }
            }
        });
        client.subscribe("/test1");

        MuleClient muleClient = muleContext.getClient();
        muleClient.dispatch("vm://in1", "Ross", null);
        assertTrue("data did not arrive on time", latch.await(DEFAULT_TEST_TIMEOUT_SECS,
                                                                    TimeUnit.SECONDS));

        assertNotNull(data.get());

        // parse the result string into java objects. different jvms return it in
        // different order, so we can't do a straight string comparison
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> result = mapper.readValue(data.get(), Map.class);
        assertEquals("/test1", result.get("channel"));
        assertEquals("Ross", ((Map<?, ?>)result.get("data")).get("name"));
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testClientPublishWithJsonObject() throws Exception
    {
        client.publish("/test2", "{\"name\":\"Ross\"}", null);

        MuleClient muleClient = muleContext.getClient();
        MuleMessage msg = muleClient.request("vm://in2", 5000L);

        assertNotNull(msg);
        assertEquals("Received: DummyJsonBean{name='Ross'}", msg.getPayloadAsString());
    }
}
