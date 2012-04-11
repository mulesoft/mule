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

import com.mockobjects.dynamic.Mock;
import org.junit.Test;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IdempotentMessageFilterMule6079TestCase extends AbstractMuleContextTestCase
{
    private Service service;
    private InboundEndpoint endpoint1;
    private Mock session;
    private ObjectStore<String> objectStore;
    private IdempotentMessageFilter ir;
    private Integer processedEvents = 0;

    /*
     * This test admits two execution paths, note that the implementation of objectStore can lock on the await call of
     * the latch, to avoid this a countDown call was added to contains method, since there is a trace that locks
     * otherwise. See implementation of IdempotentMessageFilter.isNewMessage to understand the trace.
     */
    @Test //test for MULE-6079
    public void testRaceConditionOnAcceptAndProcess() throws Exception
    {
        endpoint1 = getTestInboundEndpoint("Test1Provider", "test://Test1Provider?exchangePattern=one-way");
        session = MuleTestUtils.getMockSession();
        service = getTestService();
        session.matchAndReturn("getFlowConstruct", service);
        CountDownLatch cdl = new CountDownLatch(2);

        objectStore = new ObjectStoreWithLatch(cdl);
        ir = new IdempotentMessageFilter();
        ir.setIdExpression("#[header:id]");
        ir.setFlowConstruct(service);
        ir.setThrowOnUnaccepted(false);
        ir.setStorePrefix("foo");
        ir.setStore(objectStore);

        Thread t1 = new Thread(new TestForRaceConditionRunnable(), "thread1");
        Thread t2 = new Thread(new TestForRaceConditionRunnable(), "thread2");
        t1.start();
        t2.start();
        t1.join(5000);
        t2.join(5000);
        assertEquals("Two equal messages were processed by IdempotentMessageFilter", new Integer(1), processedEvents);
    }

    private class TestForRaceConditionRunnable implements Runnable
    {
        public TestForRaceConditionRunnable() {}

        @Override
        public void run()
        {
            MuleMessage okMessage = new DefaultMuleMessage("OK", muleContext);
            okMessage.setOutboundProperty("id", "1");
            MuleEvent event = new DefaultMuleEvent(okMessage, endpoint1, (MuleSession) session.proxy());

            try
            {
                event = ir.process(event);
            }
            catch (Throwable e)
            {
                e.printStackTrace();
                fail("An exception occurred, this should not happen. ");
            }
            
            if(event != null)
            {
                synchronized (processedEvents)  // shared
                {
                    processedEvents++;
                }
            }
        }
    }
    
    private class ObjectStoreWithLatch implements ObjectStore<String>
    {
        protected CountDownLatch barrier;
        Map<Serializable, String> map = new TreeMap<Serializable, String>();

        public ObjectStoreWithLatch(CountDownLatch latch)
        {
            barrier = latch;
        }

        @Override
        public boolean contains(Serializable key) throws ObjectStoreException 
        {
            if(key == null)
            {
                throw new ObjectStoreException();
            }
            boolean res;
            synchronized (this)
            {
                // avoiding deadlock with the latch (locks if the element was already added to map, see definition of
                // IdempotentMessageFilter.isNewMessage definition, if the element is added, it wont enter the
                // objectStore.store method, and will lock.
                res = map.containsKey(key);
                if(res)
                {
                    barrier.countDown();
                }
            }
            return res;
        }

        @Override
        public void store(Serializable key, String value) throws ObjectStoreException 
        {
            boolean wasAdded;
            if(key == null)
            {
                throw new ObjectStoreException();
            }
            synchronized (map) // map is shared
            {
                wasAdded = map.containsKey(key);
                map.put(key, value);
            }
            barrier.countDown();
            try
            {
                barrier.await();
            } catch (Exception e) {
                fail("InterruptedException, this should not happen.");
            }
            if(wasAdded)
            {
                throw new ObjectAlreadyExistsException();
            }
        }

        @Override
        public String retrieve(Serializable key) throws ObjectStoreException 
        {
            if(key == null)
            {
                throw new ObjectStoreException();
            }
            return map.get(key);
        }

        @Override
        public String remove(Serializable key) throws ObjectStoreException 
        {
            if(key == null)
            {
                throw new ObjectStoreException();
            }
            String ret = map.get(key);
            map.remove(key);
            return ret;
        }

        @Override
        public boolean isPersistent() 
        {
            return false;
        }
    }
}

