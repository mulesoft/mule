/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.store.ObjectStoreException;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.exception.AbstractExceptionListener;
import org.mule.MessageExchangePattern;
import org.mule.processor.AbstractFilteringMessageProcessor;
import org.mule.processor.AbstractInterceptingMessageProcessorBase;
import org.mule.routing.AggregationException;
import org.mule.routing.EventGroup;
import org.mule.routing.SimpleCollectionAggregator;
import org.mule.routing.correlation.CollectionCorrelatorCallback;
import org.mule.routing.correlation.CorrelationTimeoutException;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;
import org.mule.VoidMuleEvent;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.junit.Test;

/**
 * Test that aggregators preserve message order in synchronous scenarios (MULE-5998)
 */
public class AggregationTestCase extends FunctionalTestCase
{
    private static final Log logger = getLog(AbstractInterceptingMessageProcessorBase.class);

    private static Latch timeoutLatch;
    
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/aggregation-config.xml";
    }

    @Test
    public void testCollectionAggregationTimeout() throws Exception
    {
        MuleClient client = muleContext.getClient();

        String payload = "Long string that wil be broken uop into multiple messages";
        client.dispatch("vm://inTimeout", payload, null);     
        client.request("vm://collectionCreated2", 5000);
        assertThat(TestExceptionStrategy.exception, instanceOf(CorrelationTimeoutException.class));
        assertThat(TestExceptionStrategy.event, not(instanceOf(VoidMuleEvent.class)));
    }
    
    @Test
    public void testCollectionAggregator() throws Exception
    {
        MuleClient client = muleContext.getClient();

        String payload = "Long string that wil be broken uop into multiple messages";
        client.dispatch("vm://in", payload, null);
        MuleMessage msg = client.request("vm://collectionCreated", 5000);
        assertThat(msg, not(nullValue()));
        assertThat(msg, instanceOf(MuleMessageCollection.class));

        MuleMessageCollection collection = (MuleMessageCollection) msg;

        @SuppressWarnings("unchecked")
        List<byte[]> chunks = (List<byte[]>) collection.getPayload();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (byte[] chunk : chunks)
        {
            baos.write(chunk);
        }
        String aggregated = baos.toString();
        assertThat(payload, equalTo(aggregated));
    }

    @Test
    public void testCustomAggregator() throws Exception
    {
        MuleClient client = muleContext.getClient();
        String payload = "Long string that wil be broken uop into multiple messages";
        client.dispatch("vm://in2", payload, null);
        MuleMessage msg = client.request("vm://collectionCreated2", 5000);
        assertThat(msg, is(not(nullValue())));
        assertThat(msg.getPayload(), is(not(nullValue())));
        assertThat(msg.getPayload(), instanceOf(List.class));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Object obj : (List<?>)msg.getPayload())
        {
            assertThat(obj, instanceOf(MuleEvent.class));
            MuleEvent event = (MuleEvent) obj;
            assertThat(event.getMessage().getPayload(), instanceOf(byte[].class));
            baos.write((byte[])event.getMessage().getPayload());
        }
        String aggregated = baos.toString();
        assertThat(payload, equalTo(aggregated));
    }
    public static class Aggregator extends SimpleCollectionAggregator
    {
        @Override
        protected EventCorrelatorCallback getCorrelatorCallback(MuleContext context)
        {
            return new MyCollectionCorrelatorCallback(context, persistentStores, storePrefix);
        }
    }
    
    static class WaitTillTimeoutProcessor extends AbstractFilteringMessageProcessor
    {

        @Override
        protected boolean accept(MuleEvent event)
        {
            try
            {
                // The first splitted part is the one that has to create the latch
                // because at least one part has to be processed for the event group
                // to be created and the timeout exception to be spawned.
                if (timeoutLatch == null)
                {
                    timeoutLatch = new Latch();
                }
                else 
                {
                    timeoutLatch.await();
                }
            }
            catch (InterruptedException e)
            {
                logger.warn("The thread was interrupted.");
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
        
    }
    
    static class MyCollectionCorrelatorCallback extends CollectionCorrelatorCallback
    {
        public MyCollectionCorrelatorCallback(MuleContext muleContext, boolean persistentStores, String storePrefix)
        {
            super(muleContext, storePrefix);
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
    
    public static class TestExceptionStrategy extends AbstractExceptionListener implements MessagingExceptionHandler, SystemExceptionHandler
    {

        public static Exception exception;
        public static MuleEvent event;

        @Override
        public void handleException(Exception exception, RollbackSourceCallback rollbackMethod)
        {
            this.exception = exception;

        }

        @Override
        public void handleException(Exception exception)
        {
            this.exception = exception;
        }

        @Override
        public MuleEvent handleException(Exception exception, MuleEvent event)
        {
            this.event = event;
            this.exception = exception;
            timeoutLatch.countDown();
            return event;
        }

    }

}
