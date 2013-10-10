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

import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transaction.MuleTransactionConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultRouterResultsHandlerTestCase extends AbstractMuleTestCase
{

    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
    protected MuleContext muleContext = Mockito.mock(MuleContext.class);
    protected MuleSession session = Mockito.mock(MuleSession.class);
    protected InboundEndpoint endpoint = Mockito.mock(InboundEndpoint.class);

    @Before
    public void setupMocks() throws EndpointException
    {
        Mockito.when(endpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("test://test", muleContext));
        Mockito.when(endpoint.getTransactionConfig()).thenReturn(new MuleTransactionConfig());
        Mockito.when(endpoint.getExchangePattern()).thenReturn(MessageExchangePattern.ONE_WAY);
    }

    @Test
    public void aggregateNoEvent()
    {
        MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent> singletonList(null),
            Mockito.mock(MuleEvent.class), Mockito.mock(MuleContext.class));
        assertNull(result);
    }

    @Test
    public void aggregateSingleEvent()
    {
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        message1.setInvocationProperty("key1", "value1");
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, session);

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        message2.setInvocationProperty("key2", "value2");
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, session);

        MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent> singletonList(event2),
            event1, Mockito.mock(MuleContext.class));
        assertSame(event2, result);

        // Because same event instance is returned rather than MessageCollection
        // don't copy invocation properties
        assertNull(result.getMessage().getInvocationProperty("key1"));
        assertEquals("value2", result.getMessage().getInvocationProperty("key2"));
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
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, session);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, session);
        MuleEvent event3 = new DefaultMuleEvent(message3, endpoint, session);
        List<MuleEvent> events = new ArrayList<MuleEvent>();
        events.add(event2);
        events.add(event3);

        MuleEvent result = resultsHandler.aggregateResults(events, event1, Mockito.mock(MuleContext.class));
        assertNotNull(result);
        assertEquals(DefaultMessageCollection.class, result.getMessage().getClass());
        assertEquals(2, ((MuleMessageCollection)result.getMessage()).size());
        assertTrue(result.getMessage().getPayload() instanceof List<?>);
        assertEquals(message2, ((MuleMessageCollection)result.getMessage()).getMessage(0));
        assertEquals(message3, ((MuleMessageCollection)result.getMessage()).getMessage(1));

        // Because a new MuleMessageCollection is created, propagate properties from
        // original event
        assertEquals("value1", result.getMessage().getInvocationProperty("key1"));
        assertNull(result.getMessage().getInvocationProperty("key2"));
        assertNull(result.getMessage().getInvocationProperty("key3"));

        // Root id
        assertEquals(event1.getMessage().getMessageRootId(), result.getMessage().getMessageRootId());
    }

    @Test
    public void aggregateMultipleEventsAllButOneNull()
    {
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        message1.setInvocationProperty("key", "value");
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        message2.setInvocationProperty("key2", "value2");
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, session);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, session);
        List<MuleEvent> events = new ArrayList<MuleEvent>();
        events.add(null);
        events.add(event2);

        MuleEvent result = resultsHandler.aggregateResults(events, event1, Mockito.mock(MuleContext.class));
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
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, session);

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);

        MuleMessageCollection messageCollection = new DefaultMessageCollection(
            Mockito.mock(MuleContext.class));
        messageCollection.setInvocationProperty("key2", "value2");
        messageCollection.addMessage(message2);
        messageCollection.addMessage(message3);
        MuleEvent event2 = new DefaultMuleEvent(messageCollection, endpoint, session);

        MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent> singletonList(event2),
            event1, Mockito.mock(MuleContext.class));
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
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, session);

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        MuleMessage message4 = new DefaultMuleMessage("test event D", muleContext);
        MuleMessage message5 = new DefaultMuleMessage("test event E", muleContext);

        MuleMessageCollection messageCollection = new DefaultMessageCollection(
            Mockito.mock(MuleContext.class));
        messageCollection.setInvocationProperty("key2", "value2");
        messageCollection.addMessage(message2);
        messageCollection.addMessage(message3);
        MuleEvent event2 = new DefaultMuleEvent(messageCollection, endpoint, session);

        MuleMessageCollection messageCollection2 = new DefaultMessageCollection(
            Mockito.mock(MuleContext.class));
        messageCollection.setInvocationProperty("key3", "value3");
        messageCollection.addMessage(message4);
        messageCollection.addMessage(message5);
        MuleEvent event3 = new DefaultMuleEvent(messageCollection2, endpoint, session);

        List<MuleEvent> events = new ArrayList<MuleEvent>();
        events.add(event2);
        events.add(event3);

        MuleEvent result = resultsHandler.aggregateResults(events, event1, Mockito.mock(MuleContext.class));
        assertNotNull(result);
        assertEquals(DefaultMessageCollection.class, result.getMessage().getClass());
        assertEquals(2, ((MuleMessageCollection)result.getMessage()).size());
        assertTrue(result.getMessage().getPayload() instanceof List<?>);
        assertEquals(messageCollection, ((MuleMessageCollection)result.getMessage()).getMessage(0));
        assertEquals(messageCollection2, ((MuleMessageCollection)result.getMessage()).getMessage(1));

        // Because a new MuleMessageCollection is created, propagate properties from
        // original event
        assertEquals("value1", result.getMessage().getInvocationProperty("key1"));
        assertNull(result.getMessage().getInvocationProperty("key2"));
        assertNull(result.getMessage().getInvocationProperty("key3"));

        // Root id
        assertEquals(event1.getMessage().getMessageRootId(), result.getMessage().getMessageRootId());
    }

}
