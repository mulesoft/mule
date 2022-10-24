/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;


import static java.util.Collections.unmodifiableMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;

public class InstanceLockGroupTestCase extends AbstractMuleTestCase {

  public static final int THREAD_COUNT = 100;
  public static final int ITERATIONS_PER_THREAD = 100;
  private Latch threadStartLatch = new Latch();
  private String sharedKeyA = "A";
  private String sharedKeyB = "B";
  private InstanceLockGroup instanceLockGroup = new InstanceLockGroup(new SingleServerLockProvider());
  private InMemoryObjectStore objectStore = new InMemoryObjectStore();
  private LockProvider mockLockProvider;

  @Test
  public void testLockUnlock() throws Exception {
    testHighConcurrency(false);
  }

  @Test
  public void testTryLockUnlock() throws Exception {
    testHighConcurrency(true);
  }

  @Test
  public void testWhenUnlockThenDestroy() throws Exception {
    lockUnlockThenDestroy(1);
  }

  @Test
  public void testWhenSeveralLockOneUnlockThenDestroy() throws Exception {
    lockUnlockThenDestroy(5);
  }

  private void lockUnlockThenDestroy(int lockTimes) {
    mockLockProvider = Mockito.mock(LockProvider.class, Answers.RETURNS_DEEP_STUBS.get());
    InstanceLockGroup instanceLockGroup = new InstanceLockGroup(mockLockProvider);
    for (int i = 0; i < lockTimes; i++) {
      instanceLockGroup.lock("lockId");
    }
    instanceLockGroup.unlock("lockId");
    Mockito.verify(mockLockProvider, VerificationModeFactory.times(1)).createLock("lockId");
  }



  private void testHighConcurrency(boolean useTryLock) throws InterruptedException, ObjectStoreException {
    List<Thread> threads = new ArrayList<Thread>(THREAD_COUNT * 2);
    for (int i = 0; i < THREAD_COUNT; i++) {
      IncrementKeyValueThread incrementKeyValueThread = new IncrementKeyValueThread(sharedKeyA, useTryLock);
      threads.add(incrementKeyValueThread);
      incrementKeyValueThread.start();
      incrementKeyValueThread = new IncrementKeyValueThread(sharedKeyB, useTryLock);
      threads.add(incrementKeyValueThread);
      incrementKeyValueThread.start();
    }
    threadStartLatch.release();
    for (Thread thread : threads) {
      thread.join();
    }
    assertThat(objectStore.retrieve(sharedKeyA), is(THREAD_COUNT * ITERATIONS_PER_THREAD));
    assertThat(objectStore.retrieve(sharedKeyB), is(THREAD_COUNT * ITERATIONS_PER_THREAD));
  }

  public class IncrementKeyValueThread extends Thread {

    private String key;
    private boolean useTryLock;


    private IncrementKeyValueThread(String key, boolean useTryLock) {
      super("Thread-" + key);
      this.key = key;
      this.useTryLock = useTryLock;
    }

    @Override
    public void run() {
      try {
        threadStartLatch.await(5000, TimeUnit.MILLISECONDS);
        for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
          if (Thread.interrupted()) {
            break;
          }
          if (useTryLock) {
            while (!instanceLockGroup.tryLock(key, 100, TimeUnit.MILLISECONDS));
          } else {
            instanceLockGroup.lock(key);
          }
          try {
            Integer value;
            if (objectStore.contains(key)) {
              value = objectStore.retrieve(key);
              objectStore.remove(key);
            } else {
              value = 0;
            }
            objectStore.store(key, value + 1);
          } finally {
            instanceLockGroup.unlock(key);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class InMemoryObjectStore extends TemplateObjectStore<Integer> {

    private Map<String, Integer> store = new ConcurrentHashMap<>();

    @Override
    protected boolean doContains(String key) throws ObjectStoreException {
      return store.containsKey(key);
    }

    @Override
    protected void doStore(String key, Integer value) throws ObjectStoreException {
      store.put(key, value);
    }

    @Override
    protected Integer doRetrieve(String key) throws ObjectStoreException {
      return store.get(key);
    }

    @Override
    protected Integer doRemove(String key) throws ObjectStoreException {
      return store.remove(key);
    }

    @Override
    public void clear() throws ObjectStoreException {
      this.store.clear();
    }

    @Override
    public boolean isPersistent() {
      return false;
    }

    @Override
    public void open() throws ObjectStoreException {

    }

    @Override
    public void close() throws ObjectStoreException {

    }

    @Override
    public List<String> allKeys() throws ObjectStoreException {
      return new ArrayList<>(store.keySet());
    }

    @Override
    public Map<String, Integer> retrieveAll() throws ObjectStoreException {
      return unmodifiableMap(store);
    }
  }

}
