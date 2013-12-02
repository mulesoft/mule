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
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.listener.FlowExecutionListener;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
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

public class AjaxFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    public static int SERVER_PORT = -1;

    private HttpClient httpClient;
    private BayeuxClient bayeuxClient;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public AjaxFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        // start the embedded servers before starting mule to try and avoid
        // intermittent failures in testClientPublishWithString
        super(variant, configResources);
        setStartContext(false);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "ajax-embedded-functional-test-service.xml"},
            {ConfigVariant.FLOW, "ajax-embedded-functional-test-flow.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        SERVER_PORT = dynamicPort.getNumber();
        httpClient = new HttpClient();
        httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        httpClient.start();

        bayeuxClient = new BayeuxClient(httpClient, new Address("localhost", SERVER_PORT), "/ajax/cometd");
        // need to start the client before you can add subscriptions
        bayeuxClient.start();

        assertTrue("httpClient is not running", httpClient.isRunning());
        assertTrue("bayeuxClient is not running", bayeuxClient.isRunning());
        muleContext.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if(muleContext.isStarted())
        {
            muleContext.stop();
        }

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

    @Ignore("flaky test")
    @Test
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
            @Override
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

        MuleClient muleClient = muleContext.getClient();
        muleClient.dispatch("vm://in1", "Ross", null);

        latch.await(10, TimeUnit.SECONDS);
        assertNotNull(data.get());

        // parse the result string into java objects.  different jvms return it in different order, so we can't do a straight string comparison
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> result  = mapper.readValue((String) data.get(), Map.class);
        assertEquals("/test1", result.get("channel"));
        assertEquals("Ross Received", result.get("data"));
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testClientPublishWithString() throws Exception
    {
        MuleClient muleClient = muleContext.getClient();

        bayeuxClient.publish("/test2", "Ross", null);
        final MuleMessage msg = muleClient.request("vm://in2", RECEIVE_TIMEOUT * 2);
        Prober prober = new PollingProber();
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return msg != null;
            }

            @Override
            public String describeFailure()
            {
                return "No message was returned from request";
            }
        });
        assertNotNull(msg);
        assertEquals("Ross Received", msg.getPayloadAsString());
    }
}
