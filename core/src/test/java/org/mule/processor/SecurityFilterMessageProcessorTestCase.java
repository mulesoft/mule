/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.tck.security.TestSecurityFilter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class SecurityFilterMessageProcessorTestCase extends AbstractMessageProcessorTestCase
{
    @Test
    public void testFilterPass() throws Exception
    {
        TestSecurityFilter securityFilter = new TestSecurityFilter(true);
        InboundEndpoint endpoint = createTestInboundEndpoint(null, securityFilter,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        InterceptingMessageProcessor mp = new SecurityFilterMessageProcessor(securityFilter);
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
        TestSecurityFilter securityFilter = new TestSecurityFilter(false);
        InboundEndpoint endpoint = createTestInboundEndpoint(null, securityFilter,
            MessageExchangePattern.REQUEST_RESPONSE, null);
        InterceptingMessageProcessor mp = new SecurityFilterMessageProcessor(securityFilter);
        TestListener listner = new TestListener();
        mp.setListener(listner);

        MuleEvent inEvent = createTestInboundEvent(endpoint);

        // Need event in RequestContext :-(
        RequestContext.setEvent(inEvent);

        try
        {
            mp.process(inEvent);
            fail("Exception expected");
        }
        catch (TestSecurityFilter.StaticMessageUnauthorisedException e)
        {
            // expected
        }

        assertNull(listner.sensedEvent);
    }
}
