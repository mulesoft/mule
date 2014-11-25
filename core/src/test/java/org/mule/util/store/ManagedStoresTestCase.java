/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.mule.api.config.MuleProperties;
import org.mule.api.registry.RegistrationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class ManagedStoresTestCase extends AbstractMuleContextTestCase
{

    public ManagedStoresTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        MuleObjectStoreManager manager = muleContext.getRegistry().lookupObject(
            MuleProperties.OBJECT_STORE_MANAGER);
        manager.clearStoreCache();
    }

    @Test
    public void testInMemoryStore() throws ObjectStoreException, InterruptedException, RegistrationException
    {
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME,
            new SimpleMemoryObjectStore());
        ObjectStoreManager manager = muleContext.getRegistry().lookupObject(
            MuleProperties.OBJECT_STORE_MANAGER);
        ListableObjectStore store = manager.getObjectStore("inMemoryPart1", false);
        assertTrue(store instanceof PartitionedObjectStoreWrapper);
        ObjectStore baseStore = ((PartitionedObjectStoreWrapper) store).getBaseStore();
        assertTrue(baseStore instanceof SimpleMemoryObjectStore);
        assertSame(baseStore,
            muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME));
        testObjectStore(store);
        testObjectStoreExpiry(manager.getObjectStore("inMemoryExpPart1", false, ObjectStoreManager.UNBOUNDED, 500, 200));
        testObjectStoreMaxEntries((ListableObjectStore) manager.getObjectStore("inMemoryMaxPart1", false, 10,
            10000, 200));
    }

    @Test
    public void testPersistentStore()
        throws ObjectStoreException, InterruptedException, RegistrationException
    {
        QueuePersistenceObjectStore queueStore = new QueuePersistenceObjectStore(muleContext);
        queueStore.open();
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME,
            queueStore);
        ObjectStoreManager manager = muleContext.getRegistry().lookupObject(
            MuleProperties.OBJECT_STORE_MANAGER);
        ListableObjectStore store = manager.getObjectStore("persistencePart1", true);
        assertTrue(store instanceof PartitionedObjectStoreWrapper);
        ObjectStore baseStore = ((PartitionedObjectStoreWrapper) store).getBaseStore();
        assertTrue(baseStore instanceof QueuePersistenceObjectStore);
        assertSame(baseStore,
            muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME));
        testObjectStore(store);
        testObjectStoreExpiry(manager.getObjectStore("persistenceExpPart1", true, ObjectStoreManager.UNBOUNDED, 500, 200));
        testObjectStoreMaxEntries((ListableObjectStore) manager.getObjectStore("persistenceMaxPart1", true,
            10, 10000, 200));
    }

    @Test
    public void testPartitionableInMemoryStore()
        throws ObjectStoreException, RegistrationException, InterruptedException
    {
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME,
            new PartitionedInMemoryObjectStore());
        ObjectStoreManager manager = muleContext.getRegistry().lookupObject(
            MuleProperties.OBJECT_STORE_MANAGER);
        ListableObjectStore store = manager.getObjectStore("inMemoryPart2", false);
        assertTrue(store instanceof ObjectStorePartition);
        ObjectStore baseStore = ((ObjectStorePartition) store).getBaseStore();
        assertTrue(baseStore instanceof PartitionedInMemoryObjectStore);
        assertSame(baseStore,
            muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME));
        testObjectStore(store);
        testObjectStoreExpiry(manager.getObjectStore("inMemoryExpPart2", false, ObjectStoreManager.UNBOUNDED, 500, 200));
        testObjectStoreMaxEntries((ListableObjectStore) manager.getObjectStore("inMemoryMaxPart2", false, 10,
            10000, 200));
    }

    @Test
    public void testPartitionablePersistenceStore()
        throws ObjectStoreException, RegistrationException, InterruptedException
    {
        PartitionedPersistentObjectStore partitionedStore = new PartitionedPersistentObjectStore(muleContext);
        partitionedStore.open();
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME,
            partitionedStore);
        ObjectStoreManager manager = muleContext.getRegistry().lookupObject(
            MuleProperties.OBJECT_STORE_MANAGER);
        ListableObjectStore store = manager.getObjectStore("persistencePart2", true);
        assertTrue(store instanceof ObjectStorePartition);
        ObjectStore baseStore = ((ObjectStorePartition) store).getBaseStore();
        assertTrue(baseStore instanceof PartitionedPersistentObjectStore);
        assertSame(baseStore,
            muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME));
        testObjectStore(store);
        testObjectStoreExpiry(manager.getObjectStore("persistenceExpPart2", true, ObjectStoreManager.UNBOUNDED, 1000, 200));
        testObjectStoreMaxEntries((ListableObjectStore) manager.getObjectStore("persistenceMaxPart2", true,
            10, 10000, 200));
    }

    private void testObjectStore(ListableObjectStore store) throws ObjectStoreException
    {
        ObjectStoreException e = null;
        store.store("key1", "value1");
        assertEquals("value1", store.retrieve("key1"));
        assertTrue(store.contains("key1"));
        try
        {
            store.store("key1", "value1");
        }
        catch (ObjectAlreadyExistsException e1)
        {
            e = e1;
        }
        assertNotNull(e);
        e = null;
        assertEquals(1, store.allKeys().size());
        assertEquals("key1", store.allKeys().get(0));
        assertEquals("value1", store.remove("key1"));
        assertFalse(store.contains("key1"));

        try
        {
            store.retrieve("key1");
        }
        catch (ObjectDoesNotExistException e1)
        {
            e = e1;
        }
        assertNotNull(e);
        e = null;
        try
        {
            store.remove("key1");
        }
        catch (ObjectDoesNotExistException e1)
        {
            e = e1;
        }
        assertNotNull(e);
        e = null;
    }

    private void testObjectStoreExpiry(ObjectStore objectStore)
        throws ObjectStoreException, InterruptedException
    {
        objectStore.store("key1", "value1");
        assertEquals("value1", objectStore.retrieve("key1"));
        Thread.sleep(2000);
        assertFalse("Object with key1 still exists.", objectStore.contains("key1"));

    }

    private void testObjectStoreMaxEntries(ListableObjectStore objectStore)
        throws ObjectStoreException, InterruptedException
    {
        for (int i = 0; i < 100; i++)
        {
            objectStore.store("key" + i, "value" + i);
            assertEquals("value" + i, objectStore.retrieve("key" + i));
        }
        Thread.sleep(2000);
        assertEquals(10, objectStore.allKeys().size());
        for (int i = 90; i < 100; i++)
        {
            assertTrue("Checking that key" + i + " exists", objectStore.contains("key" + i));
        }

    }

}
