/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.unmodifiableMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class IdempotentMessageValidatorMule6079TestCase extends AbstractMuleContextTestCase {

  private ObjectStore<String> objectStore;
  private IdempotentMessageValidator validator;
  private AtomicInteger processedEvents = new AtomicInteger(0);
  private Boolean errorHappenedInChildThreads = false;

  /*
   * This test admits two execution paths, note that the implementation of objectStore can lock on the await call of the latch, to
   * avoid this a countDown call was added to contains method, since there is a trace that locks otherwise. See implementation of
   * IdempotentMessageValidator.isNewMessage to understand the trace.
   */
  @Test
  public void testRaceConditionOnAcceptAndProcess() throws Exception {
    CountDownLatch cdl = new CountDownLatch(2);

    objectStore = new RaceConditionEnforcingObjectStore(cdl);
    validator = new IdempotentMessageValidator();
    validator.setMuleContext(muleContext);
    validator.setStorePrefix("foo");
    validator.setObjectStore(objectStore);

    Thread t1 = new Thread(new TestForRaceConditionRunnable(), "thread1");
    Thread t2 = new Thread(new TestForRaceConditionRunnable(), "thread2");
    t1.start();
    t2.start();
    t1.join(5000);
    t2.join(5000);
    assertThat("Exception in child threads", errorHappenedInChildThreads, is(false));
    assertThat("None or more than one message was processed by IdempotentMessageValidator", processedEvents.get(), is(1));
  }

  private class TestForRaceConditionRunnable implements Runnable {

    @Override
    public void run() {
      Message okMessage = InternalMessage.builder().value("OK").build();
      BaseEventContext context = mock(BaseEventContext.class);
      when(context.getCorrelationId()).thenReturn("1");
      CoreEvent event = CoreEvent.builder(context).message(okMessage).build();

      try {
        event = validator.process(event);
      } catch (ValidationException e) {
        if (event != null) {
          processedEvents.incrementAndGet();
        }
        return;
      } catch (Throwable e) {
        e.printStackTrace();
        synchronized (errorHappenedInChildThreads) {
          errorHappenedInChildThreads = true;
        }
      }

    }
  }

  private class RaceConditionEnforcingObjectStore extends TemplateObjectStore<String> {

    protected CountDownLatch barrier;
    Map<String, String> map = new TreeMap<>();

    public RaceConditionEnforcingObjectStore(CountDownLatch latch) {
      barrier = latch;
    }

    @Override
    protected boolean doContains(String key) throws ObjectStoreException {
      boolean containsKey;
      synchronized (this) {
        // avoiding deadlock with the latch (locks if the element was already added to map, see definition of
        // IdempotentMessageValidator.isNewMessage definition, if the element is added, it wont enter the
        // objectStore.store method, and will lock.
        containsKey = map.containsKey(key);
        if (containsKey) {
          barrier.countDown();
        }
      }
      return containsKey;
    }

    @Override
    protected void doStore(String key, String value) throws ObjectStoreException {
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
    protected String doRetrieve(String key) throws ObjectStoreException {
      return null;
    }

    @Override
    protected String doRemove(String key) throws ObjectStoreException {
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

    @Override
    public void open() throws ObjectStoreException {

    }

    @Override
    public void close() throws ObjectStoreException {

    }

    @Override
    public List<String> allKeys() throws ObjectStoreException {
      return new ArrayList<>(map.keySet());
    }

    @Override
    public Map<String, String> retrieveAll() throws ObjectStoreException {
      return unmodifiableMap(map);
    }
  }
}
