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
        SensingNullMessageProcessor listener = getSensingNullMessageProcessor();
        mp.setListener(listener);

        MuleEvent inEvent = getTestEvent(TEST_MESSAGE);

        MuleEvent resultEvent = mp.process(inEvent);

        assertNull(listener.event);
        assertNull(resultEvent);
    }

    protected MessageFilter getMessageFilter(Filter filter)
    {
        return new MessageFilter(filter);
    }

}
