/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.abdera.model.Feed;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AtomFeedConsumeAndTransformTestCase extends FunctionalTestCase
{
    private Latch receiveLatch = new Latch();
    private MuleMessage message = null;

    @Override
    protected String getConfigResources()
    {
        return "atom-consume-transform-feed.xml";
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
        InputStream input = getFeedInput();

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://fromTest", input, null);

        assertTrue(receiveLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));

        Object payload = message.getPayload();
        assertTrue(payload instanceof Feed);

        Feed feed = (Feed) payload;
        assertEquals(25, feed.getEntries().size());
    }

    private InputStream getFeedInput()
    {
        InputStream input = getClass().getClassLoader().getResourceAsStream("sample-feed.atom");
        assertNotNull(input);
        return input;
    }
}
