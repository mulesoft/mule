/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.concurrent.Latch;

import java.beans.ExceptionListener;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class AsyncInterceptingMessageProcessorTestCase extends AbstractMuleTestCase
    implements ExceptionListener
{

    protected Exception exceptionThrown;
    protected Latch latch = new Latch();;

    public void testProcessSync() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE, getTestOutboundEndpoint("",
            "test://test?synchronous=true"));

        TestListener target = new TestListener();
        InterceptingMessageProcessor mp = new AsyncInterceptingMessageProcessor(muleContext.getWorkManager(),
            this);
        mp.setListener(target);

        MuleEvent result = mp.process(event);

        assertSame(event, target.sensedEvent);
        assertSame(event, result);
    }

    public void testProcessAsync() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        TestListener target = new TestListener();
        InterceptingMessageProcessor mp = new AsyncInterceptingMessageProcessor(muleContext.getWorkManager(),
            this);
        mp.setListener(target);

        MuleEvent result = mp.process(event);

        latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
        assertNotNull(target.sensedEvent);
        // Event is not the same because it gets copied in
        // AbstractMuleEventWork#run()
        assertNotSame(event, target.sensedEvent);
        assertEquals(event.getMessageAsString(), target.sensedEvent.getMessageAsString());

        assertNull(result);
        assertNull(exceptionThrown);
    }

    class TestListener implements MessageProcessor
    {
        MuleEvent sensedEvent;

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            sensedEvent = event;
            latch.countDown();
            return event;
        }
    }

    public void exceptionThrown(Exception e)
    {
        exceptionThrown = e;
    }

}
