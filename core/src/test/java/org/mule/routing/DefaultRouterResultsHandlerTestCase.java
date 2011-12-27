/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class DefaultRouterResultsHandlerTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testAggregateSingleResult() throws Exception
    {
        MuleEvent event = getTestEvent("previuos");
        MuleEvent event1 = getTestEvent("foo1");

        event.getSession().setProperty("key", "value");
        event1.getSession().setProperty("key", "valueNEW");
        event1.getSession().setProperty("key1", "value1");

        RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
        MuleEvent result = resultsHandler.aggregateResults(Collections.singletonList(event1), event,
            muleContext);

        assertNotNull(result);
        assertEquals(DefaultMuleMessage.class, result.getMessage().getClass());
        assertSame(event1, result);
        assertEquals(event1.getMessage(), result.getMessage());

        assertEquals("valueNEW", result.getSession().getProperty("key"));
        assertEquals("value1", result.getSession().getProperty("key1"));
    }

    @Test
    public void testAggregateMultipleResults() throws Exception
    {
        MuleEvent event = getTestEvent("previuos");
        MuleSession session = event.getSession();

        // Router routes that don't replace MuleSession
        MuleEvent event1 = getTestEvent("foo1", session);
        MuleEvent event2 = getTestEvent("foo2", session);
        // Router routes that replaces MuleSession
        MuleEvent event3 = getTestEvent("foo3");

        event.getSession().setProperty("key", "value");
        event1.getSession().setProperty("key1", "value1");
        event1.getSession().setProperty("key2", "value2");
        event2.getSession().setProperty("KEY2", "value2NEW");
        event2.getSession().setProperty("key3", "value3");
        event3.getSession().setProperty("key4", "value4");

        MuleEvent event4 = getTestEvent("foo5", event.getSession());

        List<MuleEvent> eventList = new ArrayList<MuleEvent>();
        eventList.add(event1);
        eventList.add(event2);
        eventList.add(event3);
        eventList.add(event4);

        RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
        MuleEvent result = resultsHandler.aggregateResults(eventList, event, muleContext);

        assertNotNull(result);
        assertEquals(DefaultMessageCollection.class, result.getMessage().getClass());
        assertEquals(4, ((MuleMessageCollection) result.getMessage()).size());
        assertEquals(event1.getMessage(), ((MuleMessageCollection) result.getMessage()).getMessage(0));
        assertEquals(event2.getMessage(), ((MuleMessageCollection) result.getMessage()).getMessage(1));
        assertEquals(event3.getMessage(), ((MuleMessageCollection) result.getMessage()).getMessage(2));
        assertEquals(event4.getMessage(), ((MuleMessageCollection) result.getMessage()).getMessage(3));

        assertEquals("value", result.getSession().getProperty("key"));
        assertEquals("value1", result.getSession().getProperty("key1"));
        assertEquals("value2NEW", result.getSession().getProperty("key2"));
        assertEquals("value3", result.getSession().getProperty("key3"));
        assertNull(result.getSession().getProperty("key4"));
    }

}
