/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.tck.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

public class InMemoryStoreTestCase extends AbstractMuleContextTestCase {

  private InMemoryObjectStore<String> store = null;

  @After
  public void disposeStore() {
    store.dispose();
  }

  @Test
  public void testSimpleTimedExpiry() throws Exception {
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

  @Test
  public void testComplexTimedExpiry() throws Exception {
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

  @Test
  public void testStoreAndRetrieve() throws Exception {
    String key = "key";
    String value = "hello";

    createBoundedObjectStore(1);

    store.store(key, value);
    assertObjectsInStore(key);

    String retrieved = store.retrieve(key);
    assertEquals(value, retrieved);

    store.remove(key);
    assertObjectsExpired(key);
  }

  @Test
  public void testExpiringUnboundedStore() throws Exception {
    createUnboundedObjectStore();

    // put some items into the store
    storeObjects("1", "2", "3");

    // expire ... this should keep all objects in the store
    store.expire();

    assertObjectsInStore("1", "2", "3");
  }

  @Test
  public void testMaxSize() throws Exception {
    int maxEntries = 3;
    createBoundedObjectStore(maxEntries);

    storeObjects("1", "2", "3");
    assertObjectsInStore("1", "2", "3");

    // exceed threshold
    store.store("4", "4");

    // the oldest entry should still be there, not yet expired
    assertTrue(store.contains("1"));

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

  private void storeObjects(String... objects) throws Exception {
    for (String entry : objects) {
      store.store(entry, entry);
    }
  }

  private void assertObjectsInStore(String... identifiers) throws Exception {
    for (String id : identifiers) {
      String message = "id " + id + " not in store " + store;
      assertTrue(message, store.contains(id));
    }
  }

  private void assertObjectsExpired(String... identifiers) throws Exception {
    for (String id : identifiers) {
      assertFalse(store.contains(id));
    }
  }

  private void createTimedObjectStore(int timeToLive) throws InitialisationException {
    int expireInterval = 1000;
    assertTrue("objects' time to live must be greater than the expire interval", timeToLive > expireInterval);

    store = new InMemoryObjectStore<>();
    store.setMuleContext(muleContext);
    store.setName("timed");
    store.setMaxEntries(3);
    store.setEntryTTL(timeToLive);
    store.setExpirationInterval(expireInterval);
    store.initialise();
  }

  private void createBoundedObjectStore(int numberOfEntries) throws InitialisationException {
    createNonExpiringObjectStore();
    store.setName("bounded");
    store.setMaxEntries(numberOfEntries);
    store.initialise();
  }

  private void createUnboundedObjectStore() throws InitialisationException {
    createNonExpiringObjectStore();
    store.setMaxEntries(-1);
    store.initialise();
  }

  private void createNonExpiringObjectStore() {
    store = new NonExpiringInMemoryObjectStore();
    store.setMuleContext(muleContext);
  }

  /**
   * Special subclass that coordinates with the expire thread. Upon calling <code>initialize</code> the scheduler in
   * {@link AbstractMonitoredObjectStore} runs once. The tests in this test case rely on the fact that no expiry happens during
   * their execution. This implementation waits for the first run of the expire method in initialize and only then continues with
   * the execution of the current thread.
   */
  private static class NonExpiringInMemoryObjectStore extends InMemoryObjectStore<String> {

    private CountDownLatch expireLatch;

    NonExpiringInMemoryObjectStore() {
      super();
      // entryTTL=-1 means we will have to expire manually
      setEntryTTL(-1);
      // run the expire thread in very, very large intervals (irrelevant to this test)
      setExpirationInterval(Integer.MAX_VALUE);

      expireLatch = new CountDownLatch(1);
    }

    @Override
    public void initialise() throws InitialisationException {
      super.initialise();

      // now wait for the first expire to happen
      try {
        expireLatch.await(30, TimeUnit.SECONDS);
      } catch (InterruptedException ie) {
        throw new RuntimeException("Interrupted while waiting for the first expire", ie);
      }
    }

    @Override
    public void expire() {
      super.expire();
      // expire successful ... signal initialize that it can continue
      expireLatch.countDown();
    }
  }
}
