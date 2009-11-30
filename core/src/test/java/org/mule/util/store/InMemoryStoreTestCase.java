/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.store;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.AbstractMuleTestCase;

public class InMemoryStoreTestCase extends AbstractMuleTestCase
{
    private InMemoryObjectStore store = null;
    
    @Override
    protected void doTearDown() throws Exception
    {
        store.dispose();
        super.doTearDown();
    }

    public void testSimpleTimedExpiry() throws Exception
    {
        int entryTTL = 3000;
        createTimedObjectStore(entryTTL);

        // store entries in quick succession
        storeObjects("1", "2", "3");

        // they should still be alive at this point
        assertObjectsInStore("1", "2", "3");

        // wait until the entry TTL has been exceeded
        Thread.sleep(entryTTL + 1000);

        // make sure all values are gone
        assertObjectsExpired("1", "2", "3");
    }
    
    public void testComplexTimedExpiry() throws Exception
    {
        int entryTTL = 3000;
        createTimedObjectStore(entryTTL);
        
        // store an entry ...
        storeObjects("1");
        
        // ... wait half of the expiry time ...
        Thread.sleep(entryTTL / 2);
        
        // ... and store another object ...
        storeObjects("2");
        
        // ... now wait until the first one is expired
        Thread.sleep((entryTTL / 2) + 500);
        
        assertObjectsExpired("1");
        assertObjectsInStore("2");
    }

    public void testStoreAndRetrieve() throws Exception
    {
        String key = "key";
        String value = "hello";
        
        createBoundedObjectStore(1);
        
        assertTrue(store.storeObject(key, value));
        assertObjectsInStore(key);
        
        Object retrieved = store.retrieveObject(key);
        assertEquals(value, retrieved);
        
        store.removeObject(key);        
        assertObjectsExpired(key);
    }
    
    public void testExpiringUnboundedStore() throws Exception
    {
        createUnboundedObjectStore();
        
        // put some items into the store
        storeObjects("1", "2", "3");
        
        // expire ... this should keep all objects in the store
        store.expire();
        
        assertObjectsInStore("1", "2", "3");
    }
        
    public void testMaxSize() throws Exception
    {
        int maxEntries = 3;
        createBoundedObjectStore(maxEntries);

        storeObjects("1", "2", "3");
        assertObjectsInStore("1", "2", "3");

        // exceed threshold
        assertTrue(store.storeObject("4", "4"));

        // the oldest entry should still be there, not yet expired
        assertTrue(store.containsObject("1"));

        // expire manually
        store.expire();
        assertObjectsExpired("1");
        assertObjectsInStore("2", "3", "4");

        // exceed some more
        storeObjects("5");
        store.expire();
        assertObjectsExpired("2");
        assertObjectsInStore("3", "4", "5");

        // exceed multiple times
        storeObjects("6", "7", "8", "9");
        store.expire();
        assertObjectsInStore("7", "8", "9");
        assertObjectsExpired("3", "4", "5", "6");
    }

    private void storeObjects(String... objects) throws Exception
    {
        for (String entry : objects)
        {
            assertTrue(store.storeObject(entry, entry));
        }
    }
    
    private void assertObjectsInStore(String... identifiers) throws Exception
    {
        for (String id : identifiers)
        {
            String message = "id " + id + " not in store " + store;
            assertTrue(message, store.containsObject(id));
        }
    }
    
    private void assertObjectsExpired(String... identifiers) throws Exception
    {
        for (String id : identifiers)
        {
            assertFalse(store.containsObject(id));
        }
    }

    private void createTimedObjectStore(int timeToLive) throws InitialisationException
    {
        int expireInterval = 1000;
        assertTrue("objects' time to live must be greater than the expire interval", 
            timeToLive > expireInterval);
        
        store = new InMemoryObjectStore();
        store.setName("timed");
        store.setMaxEntries(3);
        store.setEntryTTL(timeToLive);
        store.setExpirationInterval(expireInterval);
        store.initialise();
    }

    private void createBoundedObjectStore(int numberOfEntries) throws InitialisationException
    {
        createNonexpiringObjectStore();
        store.setName("bounded");
        store.setMaxEntries(numberOfEntries);
        store.initialise();
    }
    
    private void createUnboundedObjectStore() throws InitialisationException
    {
        createNonexpiringObjectStore();
        store.setMaxEntries(-1);
        store.initialise();
    }

    private void createNonexpiringObjectStore()
    {
        store = new InMemoryObjectStore();
        // entryTTL=-1 means we will have to expire manually
        store.setEntryTTL(-1);
        // run the expire thread in very, very large intervals (irreleavent to this test)
        store.setExpirationInterval(Integer.MAX_VALUE);
    }
}
