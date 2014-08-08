/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.store.InMemoryObjectStore;

import org.junit.Test;

public class IdempotentSecureHashMessageFilterTestCase extends AbstractMuleContextTestCase
{
    public IdempotentSecureHashMessageFilterTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testIdempotentReceiver() throws Exception
    {
        InboundEndpoint endpoint1 = getTestInboundEndpoint("Test1Provider",
            "test://Test1Provider?exchangePattern=one-way");
        Service service = getTestService();

        MuleSession session = mock(MuleSession.class);
        when(session.getFlowConstruct()).thenReturn(service);

        IdempotentSecureHashMessageFilter ir = new IdempotentSecureHashMessageFilter();
        ir.setFlowConstruct(service);
        ir.setThrowOnUnaccepted(false);
        ir.setStorePrefix("foo");
        ir.setStore(new InMemoryObjectStore<String>());
        ir.setMuleContext(muleContext);

        MuleMessage okMessage = new DefaultMuleMessage("OK", muleContext);
        MuleEvent event = new DefaultMuleEvent(okMessage, endpoint1, getTestService(), session);

        // This one will process the event on the target endpoint
        event = ir.process(event);
        assertNotNull(event);

         // This will not process, because the message is a duplicate
        okMessage = new DefaultMuleMessage("OK", muleContext);
        event = new DefaultMuleEvent(okMessage, endpoint1, getTestService(), session);
        event = ir.process(event);
        assertNull(event);

        // This will process, because the message  is not a duplicate
        okMessage = new DefaultMuleMessage("Not OK", muleContext);
        event = new DefaultMuleEvent(okMessage, endpoint1, getTestService(), session);
        event = ir.process(event);
        assertNotNull(event);
    }
}
