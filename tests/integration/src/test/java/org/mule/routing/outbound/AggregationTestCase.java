/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.store.ObjectStoreException;
import org.mule.module.client.MuleClient;
import org.mule.routing.AggregationException;
import org.mule.routing.EventGroup;
import org.mule.routing.SimpleCollectionAggregator;
import org.mule.routing.correlation.CollectionCorrelatorCallback;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.session.DefaultMuleSession;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;

/**
 * Test that aggregators preserve message order in synchronous scenarios (MULE-5998)
 */
public class AggregationTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/aggregation-config.xml";
    }
    
    @Test
    public void testCollectionAggregator() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        String payload = "Long string that wil be broken uop into multiple messages";
        client.dispatch("vm://in", payload, null);
        MuleMessage msg = client.request("vm://collectionCreated", 5000);
        assertNotNull(msg);
        assertTrue(msg instanceof MuleMessageCollection);
        MuleMessageCollection collection = (MuleMessageCollection) msg;
        List<byte[]> chunks = (List<byte[]>) collection.getPayload();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (byte[] chunk : chunks)
        {
            baos.write(chunk);
        }
        String aggregated = baos.toString();
        assertEquals(payload, aggregated);
    }

    @Test
    public void testCustomAggregator() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        String payload = "Long string that wil be broken uop into multiple messages";
        client.dispatch("vm://in2", payload, null);
        MuleMessage msg = client.request("vm://collectionCreated2", 5000);
        assertNotNull(msg);
        assertNotNull(msg.getPayload());
        assertTrue(msg.getPayload() instanceof List);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Object obj : (List)msg.getPayload())
        {
            assertTrue(obj instanceof MuleEvent);
            MuleEvent event = (MuleEvent) obj;
            assertTrue(event.getMessage().getPayload() instanceof byte[]);
            baos.write((byte[])event.getMessage().getPayload());
        }
        String aggregated = baos.toString();
        assertEquals(payload, aggregated);
    }
    public static class Aggregator extends SimpleCollectionAggregator
    {
        @Override
        protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext)
        {
            return new MyCollectionCorrelatorCallback(muleContext, persistentStores, storePrefix);
        }
    }

    static class MyCollectionCorrelatorCallback extends CollectionCorrelatorCallback
    {
        public MyCollectionCorrelatorCallback(MuleContext muleContext, boolean persistentStores, String storePrefix)
        {
            super(muleContext, persistentStores, storePrefix);
        }

        @Override
        public MuleEvent aggregateEvents(EventGroup events) throws AggregationException
        {
            List<MuleEvent> eventList = new ArrayList<MuleEvent>();
            Iterator<MuleEvent> iter = null;
            FlowConstruct fc = null;
            try
            {
                iter = events.iterator(true);
            } 
            catch (ObjectStoreException e)
            {
                throw new AggregationException(events, null, e);
            }
            while (iter.hasNext())
            {
                MuleEvent event = iter.next();
                eventList.add(event);
                fc = event.getFlowConstruct();
            }

            MuleMessage msg = new DefaultMuleMessage(eventList, muleContext);
            return new DefaultMuleEvent(msg, MessageExchangePattern.ONE_WAY, fc);
        }
    }
}
