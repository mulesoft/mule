/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
