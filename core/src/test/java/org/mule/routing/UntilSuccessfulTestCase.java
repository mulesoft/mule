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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
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
        private volatile int numberOfFailuresToSimulate;

        public MuleEvent process(final MuleEvent event) throws MuleException
        {
            eventCount++;
            if (numberOfFailuresToSimulate-- > 0)
            {
                throw new RuntimeException("simulated problem");
            }
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

        public void setNumberOfFailuresToSimulate(int numberOfFailuresToSimulate)
        {
            this.numberOfFailuresToSimulate = numberOfFailuresToSimulate;
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
        untilSuccessful.setMaxRetries(2);
        untilSuccessful.setSecondsBetweenRetries(1);

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
        ponderUntilEventProcessed(testEvent);
    }

    public void testSuccessfulDeliveryStreamPayload() throws Exception
    {
        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent(new ByteArrayInputStream("test_data".getBytes()));
        assertNull(untilSuccessful.process(testEvent));
        ponderUntilEventProcessed(testEvent);
    }

    public void testSuccessfulDeliveryAckExpression() throws Exception
    {
        untilSuccessful.setAckExpression("#[string:ACK]");
        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("test_data");
        assertEquals("ACK", untilSuccessful.process(testEvent).getMessageAsString());
        ponderUntilEventProcessed(testEvent);
    }

    public void testSuccessfulDeliveryFailureExpression() throws Exception
    {
        untilSuccessful.setFailureExpression("#[regex:(?i)error]");
        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("test_data");
        assertNull(untilSuccessful.process(testEvent));
        ponderUntilEventProcessed(testEvent);
    }

    public void testPermanentDeliveryFailure() throws Exception
    {
        targetMessageProcessor.setNumberOfFailuresToSimulate(Integer.MAX_VALUE);

        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("ERROR");
        assertNull(untilSuccessful.process(testEvent));
        ponderUntilEventAborted(testEvent);
    }

    public void testPermanentDeliveryFailureExpression() throws Exception
    {
        untilSuccessful.setFailureExpression("#[regex:(?i)error]");
        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("ERROR");
        assertNull(untilSuccessful.process(testEvent));
        ponderUntilEventAborted(testEvent);
    }

    public void testPermanentDeliveryFailureDLQ() throws Exception
    {
        targetMessageProcessor.setNumberOfFailuresToSimulate(Integer.MAX_VALUE);
        EndpointBuilder dlqEndpointBuilder = mock(EndpointBuilder.class);
        OutboundEndpoint dlqEndpoint = mock(OutboundEndpoint.class);
        when(dlqEndpointBuilder.buildOutboundEndpoint()).thenReturn(dlqEndpoint);
        untilSuccessful.setDlqEndpoint(dlqEndpointBuilder);
        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("ERROR");
        assertNull(untilSuccessful.process(testEvent));
        ponderUntilEventAborted(testEvent);

        verify(dlqEndpoint).process(any(MuleEvent.class));
    }

    public void testTemporaryDeliveryFailure() throws Exception
    {
        targetMessageProcessor.setNumberOfFailuresToSimulate(untilSuccessful.getMaxRetries());

        untilSuccessful.initialise();
        untilSuccessful.start();

        final MuleEvent testEvent = getTestEvent("ERROR");
        assertNull(untilSuccessful.process(testEvent));
        ponderUntilEventProcessed(testEvent);
        assertEquals(targetMessageProcessor.getEventCount(), untilSuccessful.getMaxRetries() + 1);
    }

    public void testPreExistingEvents() throws Exception
    {
        final MuleEvent testEvent = getTestEvent("test_data");
        objectStore.store(UntilSuccessful.EventStoreKey.buildFor(testEvent), testEvent);
        untilSuccessful.initialise();
        untilSuccessful.start();
        ponderUntilEventProcessed(testEvent);
    }

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
        while (targetMessageProcessor.getEventCount() <= untilSuccessful.getMaxRetries())
        {
            Thread.yield();
            Thread.sleep(250L);
        }

        assertEquals(0, objectStore.allKeys().size());
        assertEquals(targetMessageProcessor.getEventCount(), 1 + untilSuccessful.getMaxRetries());
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
