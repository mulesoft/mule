/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.inbound;

import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.tck.security.TestSecurityFilter;

import org.junit.Test;

public class InboundSecurityFilterMessageProcessorTestCase extends AbstractInboundMessageProcessorTestCase
{

    @Test
    public void testFilterPass() throws Exception
    {
        InboundEndpoint endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(true), 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        InterceptingMessageProcessor mp = new InboundSecurityFilterMessageProcessor(endpoint);
        TestListener listner = new TestListener();
        mp.setListener(listner);

        MuleEvent inEvent = createTestInboundEvent(endpoint);
        MuleEvent resultEvent = mp.process(inEvent);
        assertNotNull(listner.sensedEvent);
        assertSame(inEvent, listner.sensedEvent);
        assertEquals(inEvent, resultEvent);

    }

    @Test
    public void testFilterFail() throws Exception
    {
        InboundEndpoint endpoint = createTestInboundEndpoint(null, new TestSecurityFilter(false), 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        InterceptingMessageProcessor mp = new InboundSecurityFilterMessageProcessor(endpoint);
        TestListener listner = new TestListener();
        mp.setListener(listner);

        MuleEvent inEvent = createTestInboundEvent(endpoint);

        // Need event in RequestContext :-(
        RequestContext.setEvent(inEvent);

        MuleEvent resultEvent = mp.process(inEvent);
        assertNull(listner.sensedEvent);

        assertNotNull(resultEvent);
        assertEquals(TestSecurityFilter.SECURITY_EXCEPTION_MESSAGE, resultEvent.getMessageAsString());
        assertNotNull(resultEvent.getMessage().getExceptionPayload());
        assertTrue(resultEvent.getMessage().getExceptionPayload().getException() instanceof TestSecurityFilter.StaticMessageUnauthorisedException);
    }

}
