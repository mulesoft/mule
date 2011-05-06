/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.pattern.core.construct;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.pattern.core.construct.GuaranteedDeliveryInterceptingMessageProcessor.GuaranteedDeliveryPolicy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.store.SimpleMemoryObjectStore;

public class GuaranteedDeliveryInterceptingMessageProcessorTestCase extends AbstractMuleTestCase
{
    public static class ConfigurableMessageProcessor implements MessageProcessor
    {
        private MuleEvent event;

        public MuleEvent process(final MuleEvent event) throws MuleException
        {
            this.event = event;
            return event;
        }

        public MuleEvent getEventReceived()
        {
            return event;
        }
    }

    public void testSuccessfulDelivery() throws Exception
    {
        final GuaranteedDeliveryInterceptingMessageProcessor gdimp = new GuaranteedDeliveryInterceptingMessageProcessor();
        gdimp.setMuleContext(muleContext);
        gdimp.setFlowConstruct(getTestService());

        final GuaranteedDeliveryPolicy gdp = new GuaranteedDeliveryPolicy();
        gdp.setMaxDeliveryAttempts(1);
        gdp.setMillisecondsBetweenDeliveries(100L);
        final SimpleMemoryObjectStore<MuleEvent> objectStore = new SimpleMemoryObjectStore<MuleEvent>();
        gdp.setObjectStore(objectStore);

        gdimp.setGuaranteedDeliveryPolicy(gdp);
        gdimp.initialise();

        final ConfigurableMessageProcessor cmp = new ConfigurableMessageProcessor();
        gdimp.setListener(cmp);

        final MuleEvent testEvent = getTestEvent("some data");
        gdimp.process(testEvent);
        assertEquals(1, objectStore.allKeys().size());

        while (objectStore.allKeys().size() == 1)
        {
            Thread.yield();
            Thread.sleep(250L);
        }

        assertEquals(0, objectStore.allKeys().size());
        assertEquals(testEvent, cmp.getEventReceived());
    }

    // TODO (DDO) test redeliveries (eventually successful, eventually aborted)
}
