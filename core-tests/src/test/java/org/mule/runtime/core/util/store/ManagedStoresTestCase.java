/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Ignore;
import org.junit.Test;

public class ManagedStoresTestCase extends AbstractMuleContextTestCase {

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    MuleObjectStoreManager manager = muleContext.getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    manager.clearStoreCache();
  }

  @Test
  public void testInMemoryStore() throws ObjectStoreException, InterruptedException, RegistrationException {
    muleContext.getRegistry().registerObject(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME, new SimpleMemoryObjectStore<String>());
    ObjectStoreManager manager = muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_MANAGER);
    ListableObjectStore<String> store = manager.getObjectStore("inMemoryPart1", false);
    assertTrue(store instanceof PartitionedObjectStoreWrapper);
    ObjectStore<String> baseStore = ((PartitionedObjectStoreWrapper<String>) store).getBaseStore();
    assertTrue(baseStore instanceof SimpleMemoryObjectStore);
    assertSame(baseStore, muleContext.getRegistry().lookupObject(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME));
    testObjectStore(store);
    testObjectStoreExpiry(manager, manager.<ObjectStore<String>>getObjectStore("inMemoryExpPart1", false, -1, 500, 200));
    testObjectStoreMaxEntries(manager,
                              manager.<ListableObjectStore<String>>getObjectStore("inMemoryMaxPart1", false, 10, 10000, 200));
  }

  @Test
  public void testClearPartition() throws ObjectStoreException, InterruptedException, RegistrationException {
    muleContext.getRegistry().registerObject(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME, new SimpleMemoryObjectStore<String>());
    ObjectStoreManager manager = muleContext.getRegistry().lookupObject(OBJECT_STORE_MANAGER);

    ObjectStore<String> partition1 = manager.getObjectStore("inMemoryPart1", false);
    ObjectStore<String> partition2 = manager.getObjectStore("inMemoryPart2", false);

    partition1.store("key1", "value1");
    partition2.store("key2", "value2");

    assertEquals("value1", partition1.retrieve("key1"));
    assertEquals("value2", partition2.retrieve("key2"));

    partition1.clear();
    assertEquals("value2", partition2.retrieve("key2"));
  }

  @Test
  public void testPersistentStore() throws ObjectStoreException, InterruptedException, RegistrationException {
    QueuePersistenceObjectStore<String> queueStore = new QueuePersistenceObjectStore<String>(muleContext);
    queueStore.open();
    muleContext.getRegistry().registerObject(OBJECT_STORE_DEFAULT_PERSISTENT_NAME, queueStore);
    ObjectStoreManager manager = muleContext.getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    ListableObjectStore<String> store = manager.getObjectStore("persistencePart1", true);
    assertTrue(store instanceof PartitionedObjectStoreWrapper);
    ObjectStore<String> baseStore = ((PartitionedObjectStoreWrapper<String>) store).getBaseStore();
    assertTrue(baseStore instanceof QueuePersistenceObjectStore);
    assertSame(baseStore, muleContext.getRegistry().lookupObject(OBJECT_STORE_DEFAULT_PERSISTENT_NAME));
    testObjectStore(store);
    testObjectStoreExpiry(manager, manager.<ObjectStore<String>>getObjectStore("persistenceExpPart1", true, -1, 500, 200));
    testObjectStoreMaxEntries(manager,
                              manager.<ListableObjectStore<String>>getObjectStore("persistenceMaxPart1", true, 10, 10000, 200));
  }

  @Test
  public void testPartitionableInMemoryStore() throws ObjectStoreException, RegistrationException, InterruptedException {
    muleContext.getRegistry().registerObject(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME, new PartitionedInMemoryObjectStore<String>());
    ObjectStoreManager manager = muleContext.getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    ListableObjectStore<String> store = manager.getObjectStore("inMemoryPart2", false);
    assertTrue(store instanceof ObjectStorePartition);
    ObjectStore<String> baseStore = ((ObjectStorePartition<String>) store).getBaseStore();
    assertTrue(baseStore instanceof PartitionedInMemoryObjectStore);
    assertSame(baseStore, muleContext.getRegistry().lookupObject(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME));
    testObjectStore(store);
    testObjectStoreExpiry(manager, manager.<ObjectStore<String>>getObjectStore("inMemoryExpPart2", false, -1, 500, 200));
    testObjectStoreMaxEntries(manager,
                              manager.<ListableObjectStore<String>>getObjectStore("inMemoryMaxPart2", false, 10, 10000, 200));
  }

  @Ignore("MULE-6926")
  @Test
  public void testPartitionablePersistenceStore() throws ObjectStoreException, RegistrationException, InterruptedException {
    PartitionedPersistentObjectStore<String> partitionedStore = new PartitionedPersistentObjectStore<String>(muleContext);
    partitionedStore.open();
    muleContext.getRegistry().registerObject(OBJECT_STORE_DEFAULT_PERSISTENT_NAME, partitionedStore);
    ObjectStoreManager manager = muleContext.getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    ListableObjectStore<String> store = manager.getObjectStore("persistencePart2", true);
    assertTrue(store instanceof ObjectStorePartition);
    ObjectStore<String> baseStore = ((ObjectStorePartition<String>) store).getBaseStore();
    assertTrue(baseStore instanceof PartitionedPersistentObjectStore);
    assertSame(baseStore, muleContext.getRegistry().lookupObject(OBJECT_STORE_DEFAULT_PERSISTENT_NAME));
    testObjectStore(store);
    testObjectStoreExpiry(manager, manager.<ObjectStore<String>>getObjectStore("persistenceExpPart2", true, -1, 1000, 200));
    testObjectStoreMaxEntries(manager,
                              manager.<ListableObjectStore<String>>getObjectStore("persistenceMaxPart2", true, 10, 10000, 200));
  }

  private void testObjectStore(ListableObjectStore<String> store) throws ObjectStoreException {
    ObjectStoreException e = null;
    store.store("key1", "value1");
    assertEquals("value1", store.retrieve("key1"));
    assertTrue(store.contains("key1"));

    store.clear();
    assertFalse(store.contains("key1"));

    store.store("key1", "value1");

    try {
      store.store("key1", "value1");
    } catch (ObjectAlreadyExistsException e1) {
      e = e1;
    }
    assertNotNull(e);
    e = null;
    assertEquals(1, store.allKeys().size());
    assertEquals("key1", store.allKeys().get(0));
    assertEquals("value1", store.remove("key1"));
    assertFalse(store.contains("key1"));

    try {
      store.retrieve("key1");
    } catch (ObjectDoesNotExistException e1) {
      e = e1;
    }
    assertNotNull(e);
    e = null;
    try {
      store.remove("key1");
    } catch (ObjectDoesNotExistException e1) {
      e = e1;
    }
    assertNotNull(e);
    e = null;
  }

  private void testObjectStoreExpiry(ObjectStoreManager manager, ObjectStore<String> objectStore)
      throws ObjectStoreException, InterruptedException {
    try {
      objectStore.store("key1", "value1");
      assertEquals("value1", objectStore.retrieve("key1"));

      new PollingProber(2000, 50).check(new JUnitLambdaProbe(() -> {
        try {
          assertFalse("Object with key1 still exists.", objectStore.contains("key1"));
        } catch (Exception e) {
          fail(e.getMessage());
        }
        return true;
      }));
    } finally {
      manager.disposeStore(objectStore);
    }
  }

  private void testObjectStoreMaxEntries(ObjectStoreManager manager, final ListableObjectStore<String> objectStore)
      throws ObjectStoreException, InterruptedException {
    try {
      storeObjects(objectStore, 0, 90);

      ensureMillisecondChanged();

      storeObjects(objectStore, 90, 100);

      new PollingProber(2000, 50).check(new JUnitLambdaProbe(() -> {
        try {
          assertEquals(10, objectStore.allKeys().size());
          for (int i = 90; i < 100; i++) {
            assertTrue("Checking that key" + i + " exists", objectStore.contains("key" + i));
          }
        } catch (Exception e) {
          fail(e.getMessage());
        }
        return true;
      }));
    } finally {
      manager.disposeStore(objectStore);
    }

  }

  private void ensureMillisecondChanged() throws InterruptedException {
    Thread.sleep(2);
  }

  private void storeObjects(ListableObjectStore<String> objectStore, int start, int stop) throws ObjectStoreException {
    for (int i = start; i < stop; i++) {
      objectStore.store("key" + i, "value" + i);
      assertEquals("value" + i, objectStore.retrieve("key" + i));
    }
  }

}
