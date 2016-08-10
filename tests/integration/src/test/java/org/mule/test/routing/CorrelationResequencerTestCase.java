/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEventContext;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class CorrelationResequencerTestCase extends AbstractIntegrationTestCase
{
    private CountDownLatch receiveLatch = new CountDownLatch(6);

    @Override
    protected String getConfigFile()
    {
        return "correlation-resequencer-test-flow.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        FunctionalTestComponent testComponent = getFunctionalTestComponent("sorted");
        testComponent.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                receiveLatch.countDown();
            }
        });
    }

    @Test
    public void testResequencer() throws Exception
    {
        flowRunner("splitter").withPayload(Arrays.asList("a", "b", "c", "d", "e", "f")).asynchronously().run();

        FunctionalTestComponent resequencer = getFunctionalTestComponent("sorted");

        assertTrue(receiveLatch.await(3000, TimeUnit.SECONDS));

        assertEquals("Wrong number of messages received.", 6, resequencer.getReceivedMessagesCount());
        assertEquals("Sequence wasn't reordered.", "a", resequencer.getReceivedMessage(1));
        assertEquals("Sequence wasn't reordered.", "b", resequencer.getReceivedMessage(2));
        assertEquals("Sequence wasn't reordered.", "c", resequencer.getReceivedMessage(3));
        assertEquals("Sequence wasn't reordered.", "d", resequencer.getReceivedMessage(4));
        assertEquals("Sequence wasn't reordered.", "e", resequencer.getReceivedMessage(5));
        assertEquals("Sequence wasn't reordered.", "f", resequencer.getReceivedMessage(6));
    }
}
