/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.routing.filters.EqualsFilter;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class MessageFilterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testFilterPass() throws Exception
    {
        MessageFilter mp = new MessageFilter(new EqualsFilter(TEST_MESSAGE), false, null);
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
        MessageFilter mp = new MessageFilter(new EqualsFilter(null), false, null);
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
        MessageFilter mp = new MessageFilter(new EqualsFilter(TEST_MESSAGE), false, null);
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
        SensingNullMessageProcessor unaccepted = getSensingNullMessageProcessor();
        MessageFilter mp = new MessageFilter(new EqualsFilter(null), false, unaccepted);
        SensingNullMessageProcessor out = getSensingNullMessageProcessor();
        mp.setListener(out);

        MuleEvent inEvent = getTestEvent(TEST_MESSAGE, MessageExchangePattern.ONE_WAY);

        MuleEvent resultEvent = mp.process(inEvent);

        assertNull(out.event);
        assertSame(VoidMuleEvent.getInstance(), resultEvent);
        assertNotNull(unaccepted.event);
        assertSame(inEvent, unaccepted.event);
    }
}
