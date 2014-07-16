/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import com.sun.syndication.feed.synd.SyndFeed;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class RssFeedConsumeAndTransformTestCase extends FunctionalTestCase
{
    private Latch receiveLatch = new Latch();
    private MuleMessage message = null;

    @Override
    protected String getConfigFile()
    {
        return "rss-consume-transform-feed.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        FunctionalTestComponent comp = (FunctionalTestComponent)getComponent("feedTransformer");
        comp.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                message = context.getMessage();
                receiveLatch.countDown();
            }
        });
    }

    @Test
    public void testSendFeed() throws Exception
    {
        InputStream input = SampleFeed.feedAsStream();

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://fromTest", input, null);

        assertTrue(receiveLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));

        Object payload = message.getPayload();
        assertTrue(payload instanceof SyndFeed);

        SyndFeed feed = (SyndFeed) payload;
        assertEquals(25, feed.getEntries().size());
    }
}
