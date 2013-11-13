/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.routing.AsynchronousUntilSuccessfulProcessingStrategy;
import org.mule.session.DefaultMuleSession;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class PersistentStore6007TestCase extends FunctionalTestCase
{
    private static final Log log = LogFactory.getLog(PersistentStore6007TestCase.class);

    private Latch latch;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/issues/persistent-store-6007.xml";
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        setStartContext(false);
        return super.createMuleContext();
    }

    @Test
    public void testPersistentNonQueueStores() throws Exception
    {
        latch = new Latch();
        Component.latch = latch;
        PersistentObjectStore.addEvents(muleContext);
        muleContext.start();
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://input", "Hello", null);
        assertEquals("Hello", result.getPayload());
        assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
    }

    /** A store that "persists" events using keys that are not QueueEntry's */
    public static class PersistentObjectStore implements ListableObjectStore<Serializable>
    {
        private static Map<Serializable, Serializable> events = new HashMap<Serializable, Serializable>();

        static void addEvents(MuleContext context)
        {
            for (String str : new String[] {"A", "B", "C"})
            {
                MuleMessage msg = new DefaultMuleMessage(str, context);
                MuleEvent event = new DefaultMuleEvent(msg, MessageExchangePattern.ONE_WAY, null, new DefaultMuleSession());
                events.put(AsynchronousUntilSuccessfulProcessingStrategy.buildQueueKey(event), event);
            }
        }

        @Override
        public void open() throws ObjectStoreException
        {
            // does nothing
        }

        @Override
        public void close() throws ObjectStoreException
        {
            // does nothing
        }

        @Override
        public synchronized List<Serializable> allKeys() throws ObjectStoreException
        {
            return new ArrayList<Serializable>(events.keySet());
        }

        @Override
        public synchronized boolean contains(Serializable key) throws ObjectStoreException
        {
            return events.containsKey(key);
        }

        @Override
        public synchronized void store(Serializable key, Serializable value) throws ObjectStoreException
        {
            events.put(key, value);
        }

        @Override
        public synchronized Serializable retrieve(Serializable key) throws ObjectStoreException
        {
            return events.get(key);
        }

        @Override
        public synchronized Serializable remove(Serializable key) throws ObjectStoreException
        {
            return events.remove(key);
        }
        
        @Override
        public synchronized void clear() throws ObjectStoreException
        {
            events.clear();
        }

        @Override
        public boolean isPersistent()
        {
            return true;
        }
    }

    public static class Component implements Callable
    {
        private static Set<String> payloads = new HashSet<String>();
        private static Latch latch;
        private static Object lock = new Object();

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            synchronized (lock)
            {
                String payload = eventContext.getMessageAsString();
                payloads.add(payload);
                log.warn("Saw new payload: " + payload);
                log.warn("Count is now " + payloads.size());
                if (payloads.size() == 4)
                {
                    latch.countDown();
                }
                return eventContext.getMessage().getPayload();
            }
        }
    }
}
