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

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.routing.filter.FilterException;
import org.mule.processor.FilterMessageProcessor;
import org.mule.routing.filters.EqualsFilter;

import org.junit.Test;

public class FilterMessageProcessorTestCase extends AbstractInboundMessageProcessorTestCase
{

    @Test
    public void testFilterPass() throws Exception
    {
        InboundEndpoint endpoint = createTestInboundEndpoint(new EqualsFilter(TEST_MESSAGE), null, true, null);
        InterceptingMessageProcessor mp = new FilterMessageProcessor();
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
        InboundEndpoint endpoint = createTestInboundEndpoint(new EqualsFilter(null), null, true, null);
        InterceptingMessageProcessor mp = new FilterMessageProcessor();
        TestListener listner = new TestListener();
        mp.setListener(listner);

        MuleEvent inEvent = createTestInboundEvent(endpoint);
        try
        {
            MuleEvent resultEvent = mp.process(inEvent);
            fail("Filter should have thrown a FilterException");
        }
        catch (FilterException e)
        {
            // expected
        }
        assertNull(listner.sensedEvent);
    }

}
