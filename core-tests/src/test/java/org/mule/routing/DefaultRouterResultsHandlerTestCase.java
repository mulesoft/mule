/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.construct.Flow;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DefaultRouterResultsHandlerTestCase extends AbstractMuleContextTestCase
{

    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
    protected MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    protected MuleSession session = mock(MuleSession.class);
    protected Flow flow = mock(Flow.class);

    @Before
    public void setupMocks() throws Exception
    {
        when(flow.getProcessingStrategy()).thenReturn(new SynchronousProcessingStrategy());
        when(muleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
    }

    @Test
    public void aggregateNoEvent()
    {
        MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent> singletonList(null),
            mock(MuleEvent.class), muleContext);
        assertNull(result);
    }

    @Test
    public void aggregateSingleEvent()
    {

        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        message1.setInvocationProperty("key1", "value1");
        MuleEvent event1 = new DefaultMuleEvent(message1, flow);
        event1.getSession().setProperty("key", "value");

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        message2.setInvocationProperty("key2", "value2");
        MuleEvent event2 = new DefaultMuleEvent(message2, flow);
        event2.getSession().setProperty("key", "valueNEW");
        event2.getSession().setProperty("key1", "value1");

        MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent> singletonList(event2),
            event1, muleContext);
        assertSame(event2, result);

        // Because same event instance is returned rather than MessageCollection
        // don't copy invocation properties
        assertNull(result.getMessage().getInvocationProperty("key1"));
        assertEquals("value2", result.getMessage().getInvocationProperty("key2"));

        assertEquals("valueNEW", result.getSession().getProperty("key"));
        assertEquals("value1", result.getSession().getProperty("key1"));

    }

    @Test
    public void aggregateMultipleEvents() throws Exception
    {
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        message1.setInvocationProperty("key1", "value1");
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        message2.setInvocationProperty("key2", "value2");
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        message3.setInvocationProperty("key3", "value3");
        MuleEvent event1 = new DefaultMuleEvent(message1, flow);
        MuleSession session = event1.getSession();
        MuleEvent event2 = new DefaultMuleEvent(message2, flow, session);
        MuleEvent event3 = new DefaultMuleEvent(message3, flow, session);
        event1.getSession().setProperty("key", "value");
        event2.getSession().setProperty("key1", "value1");
        event2.getSession().setProperty("key2", "value2");
        event3.getSession().setProperty("KEY2", "value2NEW");
        event3.getSession().setProperty("key3", "value3");

        List<MuleEvent> events = new ArrayList<MuleEvent>();
        events.add(event2);
        events.add(event3);

        MuleEvent result = resultsHandler.aggregateResults(events, event1, muleContext);
        assertNotNull(result);
        assertEquals(2, ((List<MuleMessage>) result.getMessage().getPayload()).size());
        assertTrue(result.getMessage().getPayload() instanceof List<?>);
        assertEquals(message2, ((List<MuleMessage>) result.getMessage().getPayload()).get(0));
        assertEquals(message3, ((List<MuleMessage>) result.getMessage().getPayload()).get(1));

        // Because a new MuleMessageCollection is created, propagate properties from
        // original event
        // TODO DF
        // assertEquals("value1", result.getMessage().getInvocationProperty("key1"));
        assertNull(result.getMessage().getInvocationProperty("key2"));
        assertNull(result.getMessage().getInvocationProperty("key3"));

        // Root id
        assertEquals(event1.getMessage().getMessageRootId(), result.getMessage().getMessageRootId());

        assertEquals("value", result.getSession().getProperty("key"));
        assertEquals("value1", result.getSession().getProperty("key1"));
        assertEquals("value2NEW", result.getSession().getProperty("key2"));
        assertEquals("value3", result.getSession().getProperty("key3"));
        assertNull(result.getSession().getProperty("key4"));
    }

    @Test
    public void aggregateMultipleEventsAllButOneNull()
    {
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        message1.setInvocationProperty("key", "value");
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        message2.setInvocationProperty("key2", "value2");
        MuleEvent event1 = new DefaultMuleEvent(message1, flow);
        MuleEvent event2 = new DefaultMuleEvent(message2, flow);
        List<MuleEvent> events = new ArrayList<MuleEvent>();
        events.add(null);
        events.add(event2);

        MuleEvent result = resultsHandler.aggregateResults(events, event1, muleContext);
        assertSame(event2, result);

        // Because same event instance is returned rather than MessageCollection
        // don't copy invocation properties
        assertNull(result.getMessage().getInvocationProperty("key1"));
        assertEquals("value2", result.getMessage().getInvocationProperty("key2"));
    }

    @Test
    public void aggregateSingleMuleMessageCollection()
    {
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        message1.setInvocationProperty("key1", "value1");
        MuleEvent event1 = new DefaultMuleEvent(message1, flow);

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);

        List<MuleMessage> list = new ArrayList<>();
        list.add(message2);
        list.add(message3);
        MuleMessage messageCollection = new DefaultMuleMessage(list, muleContext);
        messageCollection.setInvocationProperty("key2", "value2");
        MuleEvent event2 = new DefaultMuleEvent(messageCollection, flow);

        MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent> singletonList(event2),
            event1, muleContext);
        assertSame(event2, result);

        // Because same event instance is returned rather than MessageCollection
        // don't copy invocation properties
        assertNull(result.getMessage().getInvocationProperty("key1"));
        assertEquals("value2", result.getMessage().getInvocationProperty("key2"));
    }

    @Test
    public void aggregateMultipleMuleMessageCollections()
    {
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        message1.setInvocationProperty("key1", "value1");
        MuleEvent event1 = new DefaultMuleEvent(message1, flow);

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        MuleMessage message4 = new DefaultMuleMessage("test event D", muleContext);
        MuleMessage message5 = new DefaultMuleMessage("test event E", muleContext);

        List<MuleMessage> list = new ArrayList<>();
        list.add(message2);
        list.add(message3);
        MuleMessage messageCollection = new DefaultMuleMessage(list, muleContext);
        messageCollection.setInvocationProperty("key2", "value2");
        MuleEvent event2 = new DefaultMuleEvent(messageCollection, flow);

        List<MuleMessage> list2 = new ArrayList<>();
        list.add(message4);
        list.add(message5);
        MuleMessage messageCollection2 = new DefaultMuleMessage(list2, muleContext);
        messageCollection.setInvocationProperty("key3", "value3");
        MuleEvent event3 = new DefaultMuleEvent(messageCollection2, flow);

        List<MuleEvent> events = new ArrayList<MuleEvent>();
        events.add(event2);
        events.add(event3);

        MuleEvent result = resultsHandler.aggregateResults(events, event1, muleContext);
        assertNotNull(result);
        assertEquals(2, ((List<MuleMessage>) result.getMessage().getPayload()).size());
        assertTrue(result.getMessage().getPayload() instanceof List<?>);
        assertEquals(messageCollection, ((List<MuleMessage>) result.getMessage().getPayload()).get(0));
        assertEquals(messageCollection2, ((List<MuleMessage>) result.getMessage().getPayload()).get(1));

        // Because a new MuleMessageCollection is created, propagate properties from
        // original event
        // TODO DF
        // assertEquals("value1", result.getMessage().getInvocationProperty("key1"));
        assertNull(result.getMessage().getInvocationProperty("key2"));
        assertNull(result.getMessage().getInvocationProperty("key3"));

        // Root id
        assertEquals(event1.getMessage().getMessageRootId(), result.getMessage().getMessageRootId());
    }

}
