/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
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

public class DispatchTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "dispatch-conf.xml";
    }

    @Test
    public void testEchoService() throws Exception
    {
        final int THREADS = 10;
        final CountDownLatch latch = new CountDownLatch(THREADS);

        final MuleClient client = muleContext.getClient();

        final byte[] buf = new byte[8192];
        Arrays.fill(buf, (byte) 'a');

        client.send(((InboundEndpoint) muleContext.getRegistry().lookupObject("inEchoService")).getAddress(),
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
                            client.dispatch(((InboundEndpoint) muleContext.getRegistry().lookupObject("inEchoService")).getAddress(),
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
