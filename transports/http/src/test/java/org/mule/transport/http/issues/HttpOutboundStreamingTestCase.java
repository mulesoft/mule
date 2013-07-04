/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.issues;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class HttpOutboundStreamingTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-streaming-outbound.xml";
    }

    @Test
    public void streamingOutbound() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphabetic(4096 * 10);
        final CountDownLatch latch = new CountDownLatch(1);

        InputStream stream = new InputStream()
        {

            private int index = 0;

            @Override
            public int available() throws IOException
            {
                return payload.length() - index;
            }

            @Override
            public int read() throws IOException
            {
                if (index == payload.length())
                {
                    return -1;
                }

                int value = payload.charAt(index);
                this.index++;

                if (index == 5000) // random position in the second chunk
                {
                    try
                    {
                        latch.await();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }

                return value;
            }
        };

        MuleMessage responseMessage = null;
        try
        {
            MuleClient client = muleContext.getClient();
            client.dispatch("vm://send", getTestEvent(stream, MessageExchangePattern.ONE_WAY).getMessage());
            responseMessage = client.request("vm://callback", 10000);
        }
        finally
        {
            latch.countDown();
        }

        Assert.assertNotNull("there was no streaming", responseMessage);
    }

}
