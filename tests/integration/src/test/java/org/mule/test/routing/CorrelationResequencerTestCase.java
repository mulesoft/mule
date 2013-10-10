/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import org.mule.api.MuleEventContext;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.Arrays;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CorrelationResequencerTestCase extends FunctionalTestCase
{

    private CountDownLatch receiveLatch = new CountDownLatch(6);
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        FunctionalTestComponent testComponent = getFunctionalTestComponent("test validator");
        testComponent.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                receiveLatch.countDown();                
            }
        });
    }

    @Override
    protected String getConfigResources()
    {
        return "correlation-resequencer-test.xml";
    }

    @Test
    public void testResequencer() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://splitter", Arrays.asList("a", "b", "c", "d", "e", "f"), null);

        FunctionalTestComponent resequencer = getFunctionalTestComponent("test validator");

        assertTrue(receiveLatch.await(30, TimeUnit.SECONDS));

        assertEquals("Wrong number of messages received.", 6, resequencer.getReceivedMessagesCount());
        assertEquals("Sequence wasn't reordered.", "a", resequencer.getReceivedMessage(1));
        assertEquals("Sequence wasn't reordered.", "b", resequencer.getReceivedMessage(2));
        assertEquals("Sequence wasn't reordered.", "c", resequencer.getReceivedMessage(3));
        assertEquals("Sequence wasn't reordered.", "d", resequencer.getReceivedMessage(4));
        assertEquals("Sequence wasn't reordered.", "e", resequencer.getReceivedMessage(5));
        assertEquals("Sequence wasn't reordered.", "f", resequencer.getReceivedMessage(6));
    }
}
