/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DispatchTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "dispatch-conf.xml";
    }

    @Test
    public void testEchoService() throws Exception
    {
        final int THREADS = 10;
        final CountDownLatch latch = new CountDownLatch(THREADS);

        final MuleClient client = new MuleClient(muleContext);

        final byte[] buf = new byte[8192];
        Arrays.fill(buf, (byte) 'a');

        client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inEchoService")).getAddress(),
            new DefaultMuleMessage(new ByteArrayInputStream(buf), muleContext));

        for (int i = 0; i < THREADS; i++)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    Map<String, Object> props = new HashMap<String, Object>();
                    props.put(MuleProperties.MULE_REPLY_TO_PROPERTY, "vm://queue");
                    try
                    {
                        for (int j = 0; j < 20; j++)
                        {
                            client.dispatch(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inEchoService")).getAddress(),
                                new DefaultMuleMessage(buf, muleContext), props);
                        }

                    }
                    catch (MuleException e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        latch.countDown();
                    }
                }

            }).start();
        }

        //wait for somewhere close to 15 seconds before the test times out
        latch.await(40, TimeUnit.SECONDS);

        int count = 0;
        while (client.request("vm://queue", RECEIVE_TIMEOUT) != null)
        {
            count++;
        }

        assertEquals(200, count);
    }
}
