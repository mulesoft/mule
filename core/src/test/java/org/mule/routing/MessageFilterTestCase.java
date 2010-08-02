/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.routing.filters.EqualsFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.SensingNullMessageProcessor;

import org.junit.Test;

public class MessageFilterTestCase extends AbstractMuleTestCase
{

    @Test
    public void testFilterPass() throws Exception
    {
        InterceptingMessageProcessor mp = getMessageFilter(new EqualsFilter(TEST_MESSAGE));
        SensingNullMessageProcessor listener = getSensingNullMessageProcessor();
        mp.setListener(listener);

        MuleEvent inEvent = getTestEvent(TEST_MESSAGE);

        MuleEvent resultEvent = mp.process(inEvent);

        assertNotNull(listener.event);
        assertSame(inEvent, listener.event);
        assertEquals(inEvent, resultEvent);
    }

    @Test
    public void testFilterFail() throws Exception
    {
        InterceptingMessageProcessor mp = getMessageFilter(new EqualsFilter(null));
        SensingNullMessageProcessor out = getSensingNullMessageProcessor();
        mp.setListener(out);

        MuleEvent inEvent = getTestEvent(TEST_MESSAGE);

        MuleEvent resultEvent = mp.process(inEvent);

        assertNull(out.event);
        assertNull(resultEvent);
    }
    
    @Test
    public void testFilterPassUnacceptedMP() throws Exception
    {
        MessageFilter mp = getMessageFilter(new EqualsFilter(TEST_MESSAGE));
        SensingNullMessageProcessor out = getSensingNullMessageProcessor();
        SensingNullMessageProcessor unaccepted = getSensingNullMessageProcessor();
        mp.setListener(out);
        mp.setUnacceptedMessageProcessor(unaccepted);

        MuleEvent inEvent = getTestEvent(TEST_MESSAGE);

        MuleEvent resultEvent = mp.process(inEvent);

        assertNotNull(out.event);
        assertSame(inEvent, out.event);
        assertEquals(inEvent, resultEvent);
        assertNull(unaccepted.event);
    }

    @Test
    public void testFilterFailUnacceptedMP() throws Exception
    {
        MessageFilter mp = getMessageFilter(new EqualsFilter(null));
        SensingNullMessageProcessor out = getSensingNullMessageProcessor();
        SensingNullMessageProcessor unaccepted = getSensingNullMessageProcessor();
        mp.setListener(out);
        mp.setUnacceptedMessageProcessor(unaccepted);

        MuleEvent inEvent = getTestEvent(TEST_MESSAGE);

        MuleEvent resultEvent = mp.process(inEvent);

        assertNull(out.event);
        assertNull(resultEvent);
        assertNotNull(unaccepted.event);
        assertSame(inEvent, unaccepted.event);
    }

    protected MessageFilter getMessageFilter(Filter filter)
    {
        return new MessageFilter(filter);
    }

}
