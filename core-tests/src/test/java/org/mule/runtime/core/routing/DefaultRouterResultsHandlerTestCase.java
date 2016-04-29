/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.endpoint.MuleEndpointURI;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategy;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
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
    protected InboundEndpoint endpoint = mock(InboundEndpoint.class);
    protected Flow flow = mock(Flow.class);

    @Before
    public void setupMocks() throws Exception
    {
        when(endpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("test://test", muleContext));
        when(endpoint.getTransactionConfig()).thenReturn(new MuleTransactionConfig());
        when(endpoint.getExchangePattern()).thenReturn(MessageExchangePattern.ONE_WAY);
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
        DefaultMuleEvent event1 = new DefaultMuleEvent(message1, flow);
        event1.populateFieldsFromInboundEndpoint(endpoint);
        event1.setFlowVariable("key1", "value1");
        event1.getSession().setProperty("key", "value");

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        DefaultMuleEvent event2 = new DefaultMuleEvent(message2, flow);
        event2.populateFieldsFromInboundEndpoint(endpoint);
        event2.setFlowVariable("key2", "value2");
        event2.getSession().setProperty("key", "valueNEW");
        event2.getSession().setProperty("key1", "value1");

        MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent> singletonList(event2),
            event1, muleContext);
        assertSame(event2, result);

        // Because same event instance is returned rather than MessageCollection
        // don't copy invocation properties
        assertNull(result.getFlowVariable("key1"));
        assertEquals("value2", result.getFlowVariable("key2"));

        assertEquals("valueNEW", result.getSession().getProperty("key"));
        assertEquals("value1", result.getSession().getProperty("key1"));

    }

    @Test
    public void aggregateMultipleEvents() throws Exception
    {
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        DefaultMuleEvent event1 = new DefaultMuleEvent(message1, flow);
        event1.populateFieldsFromInboundEndpoint(endpoint);
        event1.setFlowVariable("key1", "value1");
        MuleSession session = event1.getSession();
        DefaultMuleEvent event2 = new DefaultMuleEvent(message2, flow, session);
        event2.populateFieldsFromInboundEndpoint(endpoint);
        event2.setFlowVariable("key2", "value2");
        DefaultMuleEvent event3 = new DefaultMuleEvent(message3, flow, session);
        event3.populateFieldsFromInboundEndpoint(endpoint);
        event3.setFlowVariable("key3", "value3");
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
        assertEquals("value1", result.getFlowVariable("key1"));
        assertNull(result.getFlowVariable("key2"));
        assertNull(result.getFlowVariable("key3"));

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
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        DefaultMuleEvent event1 = new DefaultMuleEvent(message1, flow);
        event1.populateFieldsFromInboundEndpoint(endpoint);
        event1.setFlowVariable("key", "value");
        DefaultMuleEvent event2 = new DefaultMuleEvent(message2, flow);
        event2.populateFieldsFromInboundEndpoint(endpoint);
        event2.setFlowVariable("key2", "value2");
        List<MuleEvent> events = new ArrayList<MuleEvent>();
        events.add(null);
        events.add(event2);

        MuleEvent result = resultsHandler.aggregateResults(events, event1, muleContext);
        assertSame(event2, result);

        // Because same event instance is returned rather than MessageCollection
        // don't copy invocation properties
        assertNull(result.getFlowVariable("key1"));
        assertEquals("value2", result.getFlowVariable("key2"));
    }

    @Test
    public void aggregateSingleMuleMessageCollection()
    {
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        DefaultMuleEvent event1 = new DefaultMuleEvent(message1, flow);
        event1.populateFieldsFromInboundEndpoint(endpoint);
        event1.setFlowVariable("key1", "value1");

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);

        List<MuleMessage> list = new ArrayList<>();
        list.add(message2);
        list.add(message3);
        MuleMessage messageCollection = new DefaultMuleMessage(list, muleContext);
        DefaultMuleEvent event2 = new DefaultMuleEvent(messageCollection, flow);
        event2.populateFieldsFromInboundEndpoint(endpoint);
        event2.setFlowVariable("key2", "value2");

        MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent> singletonList(event2),
            event1, muleContext);
        assertSame(event2, result);

        // Because same event instance is returned rather than MessageCollection
        // don't copy invocation properties
        assertNull(result.getFlowVariable("key1"));
        assertEquals("value2", result.getFlowVariable("key2"));
    }

    @Test
    public void aggregateMultipleMuleMessageCollections()
    {
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        MuleEvent event1 = new DefaultMuleEvent(message1, flow);
        event1.setFlowVariable("key1", "value1");

        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        MuleMessage message4 = new DefaultMuleMessage("test event D", muleContext);
        MuleMessage message5 = new DefaultMuleMessage("test event E", muleContext);

        List<MuleMessage> list = new ArrayList<>();
        list.add(message2);
        list.add(message3);
        MuleMessage messageCollection = new DefaultMuleMessage(list, muleContext);
        DefaultMuleEvent event2 = new DefaultMuleEvent(messageCollection, flow);
        event2.populateFieldsFromInboundEndpoint(endpoint);
        event2.setFlowVariable("key2", "value2");

        List<MuleMessage> list2 = new ArrayList<>();
        list.add(message4);
        list.add(message5);
        MuleMessage messageCollection2 = new DefaultMuleMessage(list2, muleContext);
        DefaultMuleEvent event3 = new DefaultMuleEvent(messageCollection2, flow);
        event3.populateFieldsFromInboundEndpoint(endpoint);
        event3.setFlowVariable("key3", "value3");

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
        assertEquals("value1", result.getFlowVariable("key1"));
        assertNull(result.getFlowVariable("key2"));
        assertNull(result.getFlowVariable("key3"));

        // Root id
        assertEquals(event1.getMessage().getMessageRootId(), result.getMessage().getMessageRootId());
    }

}
