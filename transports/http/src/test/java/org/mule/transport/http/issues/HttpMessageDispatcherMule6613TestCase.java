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
import org.mule.client.DefaultLocalMuleClient;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

public class HttpMessageDispatcherMule6613TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "streaming-outbound-mule-6613.xml";
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

        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("senderTestFlow");

        MuleClient client = new DefaultLocalMuleClient(muleContext);
        flow.process(getTestEvent(stream, MessageExchangePattern.ONE_WAY)).getMessage();

        MuleMessage responseMessage = null;
        try
        {
            responseMessage = client.request("vm://callback", 600000);
        }
        finally
        {
            latch.countDown();
        }

        Assert.assertNotNull("there was no streaming", responseMessage);
        Assert.assertEquals(responseMessage.getPayloadAsString(), payload);

    }

}
