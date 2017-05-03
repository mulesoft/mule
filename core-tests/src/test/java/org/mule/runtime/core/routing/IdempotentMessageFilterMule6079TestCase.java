/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class IdempotentMessageFilterMule6079TestCase extends AbstractMuleContextTestCase {

  private ObjectStore<String> objectStore;
  private IdempotentMessageFilter idempotentMessageFilter;
  private AtomicInteger processedEvents = new AtomicInteger(0);
  private Boolean errorHappenedInChildThreads = false;

  /*
   * This test admits two execution paths, note that the implementation of objectStore can lock on the await call of the latch, to
   * avoid this a countDown call was added to contains method, since there is a trace that locks otherwise. See implementation of
   * IdempotentMessageFilter.isNewMessage to understand the trace.
   */
  @Test
  public void testRaceConditionOnAcceptAndProcess() throws Exception {
    CountDownLatch cdl = new CountDownLatch(2);

    objectStore = new RaceConditionEnforcingObjectStore(cdl);
    idempotentMessageFilter = new IdempotentMessageFilter();
    idempotentMessageFilter.setMuleContext(muleContext);
    idempotentMessageFilter.setStorePrefix("foo");
    idempotentMessageFilter.setObjectStore(objectStore);

    Thread t1 = new Thread(new TestForRaceConditionRunnable(), "thread1");
    Thread t2 = new Thread(new TestForRaceConditionRunnable(), "thread2");
    t1.start();
    t2.start();
    t1.join(5000);
    t2.join(5000);
    assertThat("Exception in child threads", errorHappenedInChildThreads, is(false));
    assertThat("None or more than one message was processed by IdempotentMessageFilter", processedEvents.get(), is(1));
  }

  private class TestForRaceConditionRunnable implements Runnable {

    @Override
    public void run() {
      Message okMessage = InternalMessage.builder().payload("OK").build();
      EventContext context = mock(EventContext.class);
      when(context.getId()).thenReturn("1");
      Event event = Event.builder(context).message(okMessage).build();

      try {
        event = idempotentMessageFilter.process(event);
      } catch (Throwable e) {
        e.printStackTrace();
        synchronized (errorHappenedInChildThreads) {
          errorHappenedInChildThreads = true;
        }
      }

      if (event != null) {
        processedEvents.incrementAndGet();
      }
    }
  }

  private class RaceConditionEnforcingObjectStore implements ObjectStore<String> {

    protected CountDownLatch barrier;
    Map<Serializable, String> map = new TreeMap<>();

    public RaceConditionEnforcingObjectStore(CountDownLatch latch) {
      barrier = latch;
    }

    @Override
    public boolean contains(Serializable key) throws ObjectStoreException {
      if (key == null) {
        throw new ObjectStoreException();
      }
      boolean containsKey;
      synchronized (this) {
        // avoiding deadlock with the latch (locks if the element was already added to map, see definition of
        // IdempotentMessageFilter.isNewMessage definition, if the element is added, it wont enter the
        // objectStore.store method, and will lock.
        containsKey = map.containsKey(key);
        if (containsKey) {
          barrier.countDown();
        }
      }
      return containsKey;
    }

    @Override
    public void store(Serializable key, String value) throws ObjectStoreException {
      boolean wasAdded;
      if (key == null) {
        throw new ObjectStoreException();
      }
      synchronized (this) // map is shared
      {
        wasAdded = map.containsKey(key);
        map.put(key, value);
      }
      barrier.countDown();
      try {
        barrier.await();
      } catch (Exception e) {
        synchronized (errorHappenedInChildThreads) {
          errorHappenedInChildThreads = true;
        }
      }
      if (wasAdded) {
        throw new ObjectAlreadyExistsException();
      }
    }

    @Override
    public String retrieve(Serializable key) throws ObjectStoreException {
      return null;
    }

    @Override
    public String remove(Serializable key) throws ObjectStoreException {
      return null;
    }

    @Override
    public boolean isPersistent() {
      return false;
    }

    @Override
    public void clear() throws ObjectStoreException {
      this.map.clear();
    }
  }
}
