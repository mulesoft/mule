/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class IdempotentMessageFilterTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testIdempotentReceiver() throws Exception
    {
        Flow flow = getTestFlow();

        MuleSession session = mock(MuleSession.class);

        InboundEndpoint endpoint1 = getTestInboundEndpoint("Test1Provider", "test://Test1Provider?exchangePattern=one-way");

        IdempotentMessageFilter ir = new IdempotentMessageFilter();
        ir.setIdExpression("#[message.inboundProperties.id]");
        ir.setFlowConstruct(flow);
        ir.setThrowOnUnaccepted(false);
        ir.setStorePrefix("foo");
        ir.setStore(new InMemoryObjectStore<String>());

        MuleMessage okMessage = new DefaultMuleMessage("OK", muleContext);
        okMessage.setOutboundProperty("id", "1");
        DefaultMuleEvent event = new DefaultMuleEvent(okMessage, getTestFlow(), session);
        event.populateFieldsFromInboundEndpoint(endpoint1);

        // This one will process the event on the target endpoint
        MuleEvent processedEvent = ir.process(event);
        assertNotNull(processedEvent);

         // This will not process, because the ID is a duplicate
        okMessage = new DefaultMuleMessage("OK", muleContext);
        okMessage.setOutboundProperty("id", "1");
        event = new DefaultMuleEvent(okMessage, getTestFlow(), session);
        event.populateFieldsFromInboundEndpoint(endpoint1);
        processedEvent = ir.process(event);
        assertNull(processedEvent);
    }
}
