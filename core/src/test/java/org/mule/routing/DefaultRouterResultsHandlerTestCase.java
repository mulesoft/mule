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
import org.mule.construct.Flow;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transaction.MuleTransactionConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultRouterResultsHandlerTestCase extends AbstractMuleContextTestCase
{

    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
    protected MuleContext muleContext = Mockito.mock(MuleContext.class);
    protected MuleSession session = Mockito.mock(MuleSession.class);
    protected InboundEndpoint endpoint = Mockito.mock(InboundEndpoint.class);
    protected Flow flow = Mockito.mock(Flow.class);

    @Before
    public void setupMocks() throws EndpointException
    {
        Mockito.when(endpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("test://test", muleContext));
        Mockito.when(endpoint.getTransactionConfig()).thenReturn(new MuleTransactionConfig());
        Mockito.when(endpoint.getExchangePattern()).thenReturn(MessageExchangePattern.ONE_WAY);
        Mockito.when(flow.getProcessingStrategy()).thenReturn(new SynchronousProcessingStrategy());
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
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, flow);
        event1.getSession().setProperty("key", "value");

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        message2.setInvocationProperty("key2", "value2");
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, flow);
        event2.getSession().setProperty("key", "valueNEW");
        event2.getSession().setProperty("key1", "value1");

        MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent> singletonList(event2),
            event1, Mockito.mock(MuleContext.class));
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
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, flow);
        MuleSession session = event1.getSession();
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, flow, session);
        MuleEvent event3 = new DefaultMuleEvent(message3, endpoint, flow, session);
        event1.getSession().setProperty("key", "value");
        event2.getSession().setProperty("key1", "value1");
        event2.getSession().setProperty("key2", "value2");
        event3.getSession().setProperty("KEY2", "value2NEW");
        event3.getSession().setProperty("key3", "value3");

        List<MuleEvent> events = new ArrayList<MuleEvent>();
        events.add(event2);
        events.add(event3);

        MuleEvent result = resultsHandler.aggregateResults(events, event1, Mockito.mock(MuleContext.class));
        assertNotNull(result);
        assertEquals(DefaultMessageCollection.class, result.getMessage().getClass());
        assertEquals(2, ((MuleMessageCollection) result.getMessage()).size());
        assertTrue(result.getMessage().getPayload() instanceof List<?>);
        assertEquals(message2, ((MuleMessageCollection) result.getMessage()).getMessage(0));
        assertEquals(message3, ((MuleMessageCollection) result.getMessage()).getMessage(1));

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
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, flow);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, flow);
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
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, flow);

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);

        MuleMessageCollection messageCollection = new DefaultMessageCollection(
            Mockito.mock(MuleContext.class));
        messageCollection.setInvocationProperty("key2", "value2");
        messageCollection.addMessage(message2);
        messageCollection.addMessage(message3);
        MuleEvent event2 = new DefaultMuleEvent(messageCollection, endpoint, flow);

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
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, flow);

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        MuleMessage message4 = new DefaultMuleMessage("test event D", muleContext);
        MuleMessage message5 = new DefaultMuleMessage("test event E", muleContext);

        MuleMessageCollection messageCollection = new DefaultMessageCollection(
            Mockito.mock(MuleContext.class));
        messageCollection.setInvocationProperty("key2", "value2");
        messageCollection.addMessage(message2);
        messageCollection.addMessage(message3);
        MuleEvent event2 = new DefaultMuleEvent(messageCollection, endpoint, flow);

        MuleMessageCollection messageCollection2 = new DefaultMessageCollection(
            Mockito.mock(MuleContext.class));
        messageCollection.setInvocationProperty("key3", "value3");
        messageCollection.addMessage(message4);
        messageCollection.addMessage(message5);
        MuleEvent event3 = new DefaultMuleEvent(messageCollection2, endpoint, flow);

        List<MuleEvent> events = new ArrayList<MuleEvent>();
        events.add(event2);
        events.add(event3);

        MuleEvent result = resultsHandler.aggregateResults(events, event1, Mockito.mock(MuleContext.class));
        assertNotNull(result);
        assertEquals(DefaultMessageCollection.class, result.getMessage().getClass());
        assertEquals(2, ((MuleMessageCollection) result.getMessage()).size());
        assertTrue(result.getMessage().getPayload() instanceof List<?>);
        assertEquals(messageCollection, ((MuleMessageCollection) result.getMessage()).getMessage(0));
        assertEquals(messageCollection2, ((MuleMessageCollection) result.getMessage()).getMessage(1));

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
