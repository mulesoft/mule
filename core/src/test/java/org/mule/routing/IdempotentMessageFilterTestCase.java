/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.store.InMemoryObjectStore;

import com.mockobjects.dynamic.Mock;

import org.junit.Test;


public class IdempotentMessageFilterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testIdempotentReceiver() throws Exception
    {
        InboundEndpoint endpoint1 = getTestInboundEndpoint("Test1Provider", "test://Test1Provider?exchangePattern=one-way");
        Mock session = MuleTestUtils.getMockSession();
        Service service = getTestService();
        session.matchAndReturn("getFlowConstruct", service);


        IdempotentMessageFilter ir = new IdempotentMessageFilter();
        ir.setIdExpression("#[header:id]");
        ir.setFlowConstruct(service);
        ir.setThrowOnUnaccepted(false);
        ir.setStorePrefix("foo");
        ir.setStore(new InMemoryObjectStore<String>());

        MuleMessage okMessage = new DefaultMuleMessage("OK", muleContext);
        okMessage.setOutboundProperty("id", "1");
        MuleEvent event = new DefaultMuleEvent(okMessage, endpoint1, getTestService(), (MuleSession) session.proxy());

        // This one will process the event on the target endpoint
        event = ir.process(event);
        assertNotNull(event);

         // This will not process, because the ID is a duplicate
        okMessage = new DefaultMuleMessage("OK", muleContext);
        okMessage.setOutboundProperty("id", "1");
        event = new DefaultMuleEvent(okMessage, endpoint1, getTestService(), (MuleSession) session.proxy());
        event = ir.process(event);
        assertNull(event);
    }

}
