/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.store.ListableObjectStore;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.store.SimpleMemoryObjectStore;

public class UntilSuccessfulTestCase extends AbstractMuleTestCase
{
    public static class ConfigurableMessageProcessor implements MessageProcessor
    {
        private volatile int eventCount;
        private volatile MuleEvent event;

        public MuleEvent process(final MuleEvent event) throws MuleException
        {
            eventCount++;
            this.event = event;
            return event;
        }

        public MuleEvent getEventReceived()
        {
            return event;
        }

        public int getEventCount()
        {
            return eventCount;
        }
    }

    private UntilSuccessful untilSuccessful;

    private ListableObjectStore<MuleEvent> objectStore;
    private ConfigurableMessageProcessor targetMessageProcessor;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        untilSuccessful = new UntilSuccessful();
        untilSuccessful.setMuleContext(muleContext);
        untilSuccessful.setFlowConstruct(getTestService());
        untilSuccessful.setMaxProcessingAttempts(2);
        untilSuccessful.setSecondsBetweenProcessingAttempts(1);

        objectStore = new SimpleMemoryObjectStore<MuleEvent>();
        untilSuccessful.setObjectStore(objectStore);

        targetMessageProcessor = new ConfigurableMessageProcessor();
        untilSuccessful.addRoute(targetMessageProcessor);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        untilSuccessful.stop();
    }

    public void testSuccessfulDelivery() throws Exception
    {
        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("test_data");
        assertNull(untilSuccessful.process(testEvent));
        assertEquals(1, objectStore.allKeys().size());
        ponderUntilEventProcessed(testEvent);
    }

    public void testSuccessfulDeliveryAckExpression() throws Exception
    {
        untilSuccessful.setAckExpression("#[string:ACK]");
        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("test_data");
        assertEquals("ACK", untilSuccessful.process(testEvent).getMessageAsString());
        assertEquals(1, objectStore.allKeys().size());
        ponderUntilEventProcessed(testEvent);
    }

    public void testSuccessfulDeliveryFailureExpression() throws Exception
    {
        untilSuccessful.setFailureExpression("#[regex: (?i)error]");
        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("test_data");
        assertNull(untilSuccessful.process(testEvent));
        assertEquals(1, objectStore.allKeys().size());
        ponderUntilEventProcessed(testEvent);
    }

    public void testPermanentDeliveryFailureExpression() throws Exception
    {
        untilSuccessful.setFailureExpression("#[regex:(?i)error]");
        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("ERROR");
        assertNull(untilSuccessful.process(testEvent));
        assertEquals(1, objectStore.allKeys().size());
        ponderUntilEventAborted(testEvent);
    }

    // TODO (DDO) test redeliveries eventually successful

    private void ponderUntilEventProcessed(final MuleEvent testEvent)
        throws InterruptedException, MuleException
    {
        while (targetMessageProcessor.getEventReceived() == null)
        {
            Thread.yield();
            Thread.sleep(250L);
        }

        assertEquals(0, objectStore.allKeys().size());
        assertLogicallyEqualEvents(testEvent, targetMessageProcessor.getEventReceived());
    }

    private void ponderUntilEventAborted(final MuleEvent testEvent)
        throws InterruptedException, MuleException
    {
        while (targetMessageProcessor.getEventCount() < untilSuccessful.getMaxProcessingAttempts())
        {
            Thread.yield();
            Thread.sleep(250L);
        }

        assertEquals(0, objectStore.allKeys().size());
        assertLogicallyEqualEvents(testEvent, targetMessageProcessor.getEventReceived());
    }

    private void assertLogicallyEqualEvents(final MuleEvent testEvent, MuleEvent eventReceived)
        throws MuleException
    {
        // events have been rewritten so are different but the correlation ID has been carried around
        assertEquals(testEvent.getMessage().getCorrelationId(), eventReceived.getMessage().getCorrelationId());
        // and their payload
        assertEquals(testEvent.getMessageAsString(), eventReceived.getMessageAsString());
    }
}
