/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreSettings.unmanagedPersistent;
import static org.mule.runtime.api.store.ObjectStoreSettings.unmanagedTransient;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.api.store.SimpleMemoryObjectStore;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.store.PartitionedInMemoryObjectStore;
import org.mule.runtime.core.internal.store.PartitionedPersistentObjectStore;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Ignore;
import org.junit.Test;

import io.qameta.allure.Issue;

public class ManagedStoresTestCase extends AbstractMuleContextTestCase {

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    MuleObjectStoreManager manager = getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    manager.clearStoreCache();
  }

  @Test
  public void testInMemoryStore() throws ObjectStoreException, InterruptedException, RegistrationException {
    getRegistry().registerObject(BASE_IN_MEMORY_OBJECT_STORE_KEY,
                                 new SimpleMemoryObjectStore<String>());
    ObjectStoreManager manager = getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    ObjectStore<String> store = manager.createObjectStore("inMemoryPart1", unmanagedTransient());
    testObjectStore(store);

    testObjectStoreExpiry(manager, "inMemoryExpPart1", ObjectStoreSettings.builder()
        .persistent(false)
        .entryTtl(500L)
        .expirationInterval(200L)
        .build());

    testObjectStoreMaxEntries(manager, "inMemoryMaxPart1", ObjectStoreSettings.builder()
        .persistent(false)
        .maxEntries(10)
        .entryTtl(10000L)
        .expirationInterval(200L)
        .build());
  }

  @Test
  public void testClearPartition() throws ObjectStoreException, InterruptedException, RegistrationException {
    getRegistry().registerObject(BASE_IN_MEMORY_OBJECT_STORE_KEY,
                                 new SimpleMemoryObjectStore<String>());
    ObjectStoreManager manager = getRegistry().lookupObject(OBJECT_STORE_MANAGER);

    ObjectStore<String> partition1 = manager.createObjectStore("inMemoryPart1", unmanagedTransient());
    ObjectStore<String> partition2 = manager.createObjectStore("inMemoryPart2", unmanagedTransient());

    partition1.store("key1", "value1");
    partition2.store("key2", "value2");

    assertEquals("value1", partition1.retrieve("key1"));
    assertEquals("value2", partition2.retrieve("key2"));

    partition1.clear();
    assertEquals("value2", partition2.retrieve("key2"));
  }

  @Test
  public void testPartitionableInMemoryStore() throws ObjectStoreException, RegistrationException, InterruptedException {
    getRegistry().registerObject(BASE_IN_MEMORY_OBJECT_STORE_KEY,
                                 new PartitionedInMemoryObjectStore<String>());
    ObjectStoreManager manager = getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    ObjectStore<String> store = manager.createObjectStore("inMemoryPart2", unmanagedTransient());
    assertTrue(store instanceof ObjectStorePartition);
    ObjectStore<String> baseStore = ((ObjectStorePartition<String>) store).getBaseStore();
    assertTrue(baseStore instanceof PartitionedInMemoryObjectStore);
    testObjectStore(store);
    testObjectStoreExpiry(manager, "inMemoryExpPart2", ObjectStoreSettings.builder()
        .persistent(false)
        .entryTtl(500L)
        .expirationInterval(200L)
        .build());
    testObjectStoreMaxEntries(manager, "inMemoryMaxPart2", ObjectStoreSettings.builder()
        .persistent(false)
        .maxEntries(10)
        .entryTtl(10000L)
        .expirationInterval(200L)
        .build());
  }

  @Ignore("MULE-6926")
  @Issue("MULE-6926")
  @Test
  public void testPartitionablePersistenceStore() throws ObjectStoreException, RegistrationException, InterruptedException {
    PartitionedPersistentObjectStore<String> partitionedStore = new PartitionedPersistentObjectStore<>(muleContext);
    partitionedStore.open();
    getRegistry().registerObject(BASE_PERSISTENT_OBJECT_STORE_KEY, partitionedStore);
    ObjectStoreManager manager = getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    ObjectStore<String> store = manager.createObjectStore("persistencePart2", unmanagedPersistent());
    assertTrue(store instanceof ObjectStorePartition);
    ObjectStore<String> baseStore = ((ObjectStorePartition<String>) store).getBaseStore();
    assertTrue(baseStore instanceof PartitionedPersistentObjectStore);
    assertSame(baseStore, getRegistry().lookupObject(BASE_PERSISTENT_OBJECT_STORE_KEY));
    testObjectStore(store);
    testObjectStoreExpiry(manager, "persistenceExpPart2", ObjectStoreSettings.builder()
        .persistent(true)
        .entryTtl(1000L)
        .expirationInterval(200L)
        .build());
    testObjectStoreMaxEntries(manager, "persistenceMaxPart2", ObjectStoreSettings.builder()
        .persistent(true)
        .maxEntries(10)
        .entryTtl(10000L)
        .expirationInterval(200L)
        .build());
  }

  private MuleRegistry getRegistry() {
    return ((MuleContextWithRegistries) muleContext).getRegistry();
  }

  private void testObjectStore(ObjectStore<String> store) throws ObjectStoreException {
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
  }

  private void testObjectStoreExpiry(ObjectStoreManager manager, String storeName, ObjectStoreSettings settings)
      throws ObjectStoreException, InterruptedException {
    ObjectStore<String> objectStore = manager.createObjectStore(storeName, settings);
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
      manager.disposeStore(storeName);
    }
  }

  private void testObjectStoreMaxEntries(ObjectStoreManager manager, String storeName, ObjectStoreSettings settings)
      throws ObjectStoreException, InterruptedException {
    ObjectStore<String> objectStore = manager.createObjectStore(storeName, settings);
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
      manager.disposeStore(storeName);
    }
  }

  private void ensureMillisecondChanged() throws InterruptedException {
    Thread.sleep(2);
  }

  private void storeObjects(ObjectStore<String> objectStore, int start, int stop) throws ObjectStoreException {
    for (int i = start; i < stop; i++) {
      objectStore.store("key" + i, "value" + i);
      assertEquals("value" + i, objectStore.retrieve("key" + i));
    }
  }

}
