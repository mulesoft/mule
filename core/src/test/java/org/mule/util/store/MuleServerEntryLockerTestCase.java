/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.junit.Test;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.concurrent.Latch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MuleServerEntryLockerTestCase
{
    public static final int THREAD_COUNT = 100;
    public static final int ITERATIONS_PER_THREAD = 1000;
    private String sharedKeyA = "A";
    private String sharedKeyB = "B";
    private MuleServerEntryLocker lockableObjectStore = new MuleServerEntryLocker();
    private InMemoryObjectStore objectStore  = new InMemoryObjectStore();
    private Latch threadStartLatch = new Latch();


    
    @Test
    public void testHighConcurrency() throws Exception
    {
        List<Thread> threads = new ArrayList<Thread>(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++)
        {
            IncrementKeyValueThread incrementKeyValueThread = new IncrementKeyValueThread(sharedKeyA);
            threads.add(incrementKeyValueThread);
            incrementKeyValueThread.start();
            incrementKeyValueThread = new IncrementKeyValueThread(sharedKeyB);
            threads.add(incrementKeyValueThread);
            incrementKeyValueThread.start();
        }
        threadStartLatch.release();
        for (Thread thread : threads)
        {
            thread.join();
        }
        assertThat(objectStore.retrieve(sharedKeyA), is(THREAD_COUNT * ITERATIONS_PER_THREAD));
        assertThat(objectStore.retrieve(sharedKeyB), is(THREAD_COUNT * ITERATIONS_PER_THREAD));
    }
    
    public class IncrementKeyValueThread extends Thread
    {
        private String key;

        public IncrementKeyValueThread(String key)
        {
            super("Thread-" + key);
            this.key = key;
        }

        @Override
        public void run()
        {
            try
            {
                threadStartLatch.await(5000, TimeUnit.MILLISECONDS);
                for (int i = 0; i < ITERATIONS_PER_THREAD; i ++)
                {
                    try
                    {
                        lockableObjectStore.lock(key);
                        Integer value;
                        if (objectStore.contains(key))
                        {
                            value = objectStore.retrieve(key);
                            objectStore.remove(key);
                        }
                        else
                        {
                            value = 0;
                        }
                        objectStore.store(key,value + 1);
                    }
                    finally
                    {
                        lockableObjectStore.release(key);
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static class InMemoryObjectStore implements ObjectStore<Integer>
    {
        private Map<Serializable,Integer> store = new HashMap<Serializable,Integer>();

        @Override
        public boolean contains(Serializable key) throws ObjectStoreException
        {
            return store.containsKey(key);
        }

        @Override
        public void store(Serializable key, Integer value) throws ObjectStoreException
        {
            if (store.containsKey(key))
            {
                throw new ObjectAlreadyExistsException(CoreMessages.createStaticMessage(""));
            }
            store.put(key,value);
        }

        @Override
        public Integer retrieve(Serializable key) throws ObjectStoreException
        {
            return store.get(key);
        }

        @Override
        public Integer remove(Serializable key) throws ObjectStoreException
        {
            return store.remove(key);
        }

        @Override
        public boolean isPersistent()
        {
            return false;
        }
    }

}
