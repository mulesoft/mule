/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.util.queue;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.internal.util.queue.AbstractQueueManager;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.Queue;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransactionQueueManagerTestCase extends AbstractMuleContextTestCase {

  public static final int THREAD_EXECUTION_TIMEOUT = 2000;
  /**
   * logger used by this class
   */
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  protected QueueTestComponent disposeTest = new QueueTestComponent();

  protected abstract AbstractQueueManager createQueueManager() throws Exception;

  protected abstract boolean isPersistent();

  @Test
  public void testPutTake() throws Exception {
    QueueManager mgr = createQueueManager();
    mgr.start();

    QueueSession s = mgr.getQueueSession();
    Queue q = s.getQueue("queue1");

    assertEquals("Queue size", 0, q.size());
    q.put("String1");
    assertEquals("Queue size", 1, q.size());
    Object o = q.take();
    assertNotNull(o);
    assertEquals("Queue content", "String1", o);
    assertEquals("Queue size", 0, q.size());

    mgr.stop();
  }

  @Test
  public void testTakePut() throws Exception {
    final QueueManager mgr = createQueueManager();
    mgr.start();

    final Latch latch = new Latch();

    Thread t = new Thread() {

      @Override
      public void run() {
        try {
          latch.countDown();
          Thread.sleep(200);
          QueueSession s = mgr.getQueueSession();
          Queue q = s.getQueue("queue1");
          assertEquals("Queue size", 0, q.size());
          q.put("String1");
        } catch (Exception e) {
          // ignore, let test fail
        }
      }
    };
    t.start();
    latch.await();
    long t0 = System.currentTimeMillis();
    QueueSession s = mgr.getQueueSession();
    Queue q = s.getQueue("queue1");
    assertEquals("Queue size", 0, q.size());
    Object o = q.take();
    long t1 = System.currentTimeMillis();
    t.join();
    assertNotNull(o);
    assertEquals("Queue content", "String1", o);
    assertEquals("Queue size", 0, q.size());
    assertTrue(t1 - t0 > 100);

    mgr.stop();
  }

  @Test
  public void testPutTakeUntake() throws Exception {
    final QueueManager mgr = createQueueManager();
    mgr.start();

    final Latch latch = new Latch();

    Thread t = new Thread() {

      @Override
      public void run() {
        try {
          latch.countDown();
          Thread.sleep(200);
          QueueSession s = mgr.getQueueSession();
          Queue q = s.getQueue("queue1");
          assertEquals("Queue size", 0, q.size());
          q.put("String1");
          q.put("String2");
        } catch (Exception e) {
          // ignore, let test fail
        }
      }
    };
    t.start();
    latch.await();
    long t0 = System.currentTimeMillis();
    QueueSession s = mgr.getQueueSession();
    Queue q = s.getQueue("queue1");
    assertEquals("Queue size", 0, q.size());
    Serializable o = q.take();
    long t1 = System.currentTimeMillis();
    t.join();
    assertNotNull(o);
    assertEquals("Queue content", "String1", o);
    assertEquals("Queue size", 1, q.size());
    assertTrue(t1 - t0 > 100);

    // Same as put/take until now, but now we do an untake
    q.untake(o);
    // Ensure queue size is now 2
    assertEquals("Queue size", 2, q.size());
    // Take to ensure order is correct
    Object o2 = q.take();
    assertEquals("Queue content", "String1", o2);
    assertEquals("Queue size", 1, q.size());

    mgr.stop();

  }

  @Test
  public void testClearWithoutTransaction() throws Exception {
    final QueueManager mgr = createQueueManager();
    mgr.start();

    QueueSession s = mgr.getQueueSession();
    Queue q = s.getQueue("queue1");
    assertEquals("Queue size", 0, q.size());
    q.put("String1");
    assertEquals("Queue size", 1, q.size());
    q.clear();
    assertEquals("Queue size", 0, q.size());

    mgr.stop();
  }

  @Test
  public void testClearInTransaction() throws Exception {
    final QueueManager mgr = createQueueManager();
    mgr.start();

    QueueSession s = mgr.getQueueSession();

    // insert item in transaction
    s.begin();
    Queue q = s.getQueue("queue1");
    assertEquals("Queue size", 0, q.size());
    q.put("String1");
    s.commit();

    assertEquals("Queue size", 1, q.size());

    // clear queue but rollback
    s.begin();
    assertEquals("Queue size", 1, q.size());
    q.clear();
    s.rollback();
    assertEquals("Queue size", 1, q.size());

    // do clear in transaction
    s.begin();
    assertEquals("Queue size", 1, q.size());
    q.clear();
    s.commit();
    assertEquals("Queue size", 0, q.size());

    mgr.stop();
  }

  @Test
  public void testTakePutRollbackPut() throws Exception {
    final QueueManager mgr = createQueueManager();
    mgr.start();

    final Latch latch = new Latch();

    Thread t = new Thread() {

      @Override
      public void run() {
        try {
          latch.countDown();
          Thread.sleep(200);
          QueueSession s = mgr.getQueueSession();
          Queue q = s.getQueue("queue1");
          assertEquals("Queue size", 0, q.size());
          s.begin();
          q.put("String1");
          s.rollback();
          s.begin();
          q.put("String2");
          s.commit();
        } catch (Exception e) {
          // ignore, let test fail
        }
      }
    };
    t.start();
    latch.await();
    long t0 = System.currentTimeMillis();
    QueueSession s = mgr.getQueueSession();
    Queue q = s.getQueue("queue1");
    assertEquals("Queue size", 0, q.size());
    Object o = q.take();
    long t1 = System.currentTimeMillis();
    t.join();
    assertNotNull(o);
    assertEquals("Queue content", "String2", o);
    assertEquals("Queue size", 0, q.size());
    assertTrue(t1 - t0 > 100);

    mgr.stop();
  }

  @Test
  public void testPutTakeUntakeRollbackUntake() throws Exception {
    final QueueManager mgr = createQueueManager();
    mgr.start();

    final Latch latch = new Latch();

    final Serializable object1 = "string1";
    final Serializable object2 = "string2";

    Thread t = new Thread() {

      @Override
      public void run() {
        try {
          latch.countDown();
          Thread.sleep(200);
          QueueSession s = mgr.getQueueSession();
          Queue q = s.getQueue("queue1");
          assertEquals("Queue size", 0, q.size());

          s.begin();
          q.untake(object1);
          s.commit();

          s.begin();
          q.untake(object2);
          s.rollback();
        } catch (Exception e) {
          // ignore, let test fail
        }
      }
    };
    t.start();
    latch.await();
    long t0 = System.currentTimeMillis();
    QueueSession s = mgr.getQueueSession();
    Queue q = s.getQueue("queue1");
    assertEquals("Queue size", 0, q.size());
    Object o = q.take();
    long t1 = System.currentTimeMillis();
    t.join();
    assertNotNull(o);
    assertEquals("Queue content", object1, o);
    assertEquals("Queue size", 0, q.size());
    assertTrue(t1 - t0 > 100);

    mgr.stop();
  }

  @Test
  public void testTakePutOverCapacity() throws Exception {
    final QueueManager mgr = createQueueManager();
    mgr.start();
    mgr.setDefaultQueueConfiguration(new DefaultQueueConfiguration(2, false));

    final Latch latch = new Latch();

    Thread t = new Thread() {

      @Override
      public void run() {
        try {
          latch.await();
          Thread.sleep(200);
          QueueSession s = mgr.getQueueSession();
          Queue q = s.getQueue("queue1");
          Object o = q.take();
          assertEquals("Queue content", "String1", o);
        } catch (Exception e) {
          // ignore, let test fail
        }
      }
    };
    t.start();

    QueueSession s = mgr.getQueueSession();
    Queue q = s.getQueue("queue1");
    assertEquals("Queue size", 0, q.size());
    q.put("String1");
    q.put("String2");

    latch.countDown();

    long t0 = System.currentTimeMillis();
    q.put("String3");
    long t1 = System.currentTimeMillis();

    t.join();

    assertEquals("Queue size", 2, q.size());
    assertTrue(t1 - t0 > 100);

    mgr.stop();
  }

  @Test
  public void testPutWithPersistence() throws Exception {
    if (isPersistent()) {
      AbstractQueueManager mgr = createQueueManager();

      try {
        QueueSession s = mgr.getQueueSession();
        mgr.start();
        Queue q = s.getQueue("queue1");
        q.put("String1");
        assertEquals("Queue size", 1, q.size());

        q = s.getQueue("queue1");
        assertEquals("Queue size", 1, q.size());
      } finally {
        mgr.stop();
        mgr.dispose();
      }

      mgr = createQueueManager();
      try {
        QueueSession s = mgr.getQueueSession();
        mgr.start();
        Queue q = s.getQueue("queue1");
        assertEquals("Queue size", 1, q.size());

      } finally {
        mgr.stop();
        mgr.dispose();
      }
    } else {
      logger.info("Ignoring test because queue manager is not persistent");
    }
  }

  @Test
  public void testTransactedPutCommitWithPersistence() throws Exception {
    if (isPersistent()) {
      AbstractQueueManager mgr = createQueueManager();

      try {
        QueueSession s = mgr.getQueueSession();
        Queue q = s.getQueue("queue1");
        mgr.start();
        s.begin();
        q.put("String1");
        assertEquals("Queue size", 1, q.size());
        s.commit();
        assertEquals("Queue size", 1, q.size());

        s = mgr.getQueueSession();
        q = s.getQueue("queue1");
        assertEquals("Queue size", 1, q.size());

        mgr.stop();

        mgr = createQueueManager();
        s = mgr.getQueueSession();
        q = s.getQueue("queue1");
        mgr.start();
        assertEquals("Queue size", 1, q.size());

      } finally {
        mgr.stop();
        mgr.dispose();
      }
    } else {
      logger.info("Ignoring test because queue manager is not persistent");
    }
  }

  @Test
  public void testTransactedPutRollbackWithPersistence() throws Exception {
    if (isPersistent()) {
      AbstractQueueManager mgr = createQueueManager();

      try {
        mgr.start();

        QueueSession s = mgr.getQueueSession();
        Queue q = s.getQueue("queue1");
        s.begin();
        q.put("String1");
        assertEquals("Queue size", 1, q.size());
        s.rollback();
        assertEquals("Queue size", 0, q.size());

        s = mgr.getQueueSession();
        q = s.getQueue("queue1");
        assertEquals("Queue size", 0, q.size());

        mgr.stop();

        mgr = createQueueManager();
        mgr.start();
        s = mgr.getQueueSession();
        q = s.getQueue("queue1");
        assertEquals("Queue size", 0, q.size());

      } finally {

        mgr.stop();
        mgr.dispose();
      }
    } else {
      logger.info("Ignoring test because queue manager is not persistent");
    }
  }

  @Test
  public void testPutTake_RespectsOrderOnPersistence() throws Exception {
    if (isPersistent()) {
      AbstractQueueManager mgr1 = createQueueManager();

      QueueSession s1 = mgr1.getQueueSession();
      Queue q1 = s1.getQueue("queue1");
      mgr1.start();
      assertEquals("Queue size", 0, q1.size());
      final int numberOfElements = 10;
      for (int i = 1; i <= numberOfElements; i++) {
        q1.put("String" + i);
        assertEquals("Queue size", i, q1.size());
      }

      mgr1.stop();

      AbstractQueueManager mgr2 = createQueueManager();

      QueueSession s2 = mgr2.getQueueSession();
      Queue q2 = s2.getQueue("queue1");
      mgr2.start();
      for (int i = 1; i <= numberOfElements; i++) {
        Object o = q2.take();
        assertNotNull(o);
        assertEquals("Queue content", "String" + i, o);
      }
      assertEquals("Queue size", 0, q2.size());

      mgr2.stop();
      mgr2.dispose();
    }
  }

  @Test
  public void testTransactionsOnMultipleQueues() throws Exception {

    AbstractQueueManager mgr = createQueueManager();

    try {
      mgr.start();

      QueueSession s1 = mgr.getQueueSession();
      QueueSession s2 = mgr.getQueueSession();

      Queue q1s1 = s1.getQueue("queue1");
      Queue q1s2 = s2.getQueue("queue1");
      Queue q2s1 = s1.getQueue("queue2");
      Queue q2s2 = s2.getQueue("queue2");

      q1s1.put("String1");
      assertEquals("Queue size", 1, q1s1.size());
      assertEquals("Queue size", 1, q1s2.size());

      s1.begin();

      Object o = q1s1.take();
      assertNotNull(o);
      assertEquals("String1", o);
      assertEquals("Queue size", 0, q1s1.size());
      assertEquals("Queue size", 0, q1s2.size());
      q2s1.put("String2");
      assertEquals("Queue size", 1, q2s1.size());
      assertEquals("Queue size", 0, q2s2.size());

      s1.commit();

      assertEquals("Queue size", 0, q1s1.size());
      assertEquals("Queue size", 0, q1s2.size());
      assertEquals("Queue size", 1, q2s1.size());
      assertEquals("Queue size", 1, q2s2.size());

      s1.begin();

      o = q2s1.take();
      assertNotNull(o);
      assertEquals("String2", o);
      assertEquals("Queue size", 0, q1s1.size());
      assertEquals("Queue size", 0, q1s2.size());
      assertEquals("Queue size", 0, q2s1.size());
      assertEquals("Queue size", 0, q2s2.size());

      q1s1.put("String1");

      assertEquals("Queue size", 1, q1s1.size());
      assertEquals("Queue size", 0, q1s2.size());
      assertEquals("Queue size", 0, q2s1.size());
      assertEquals("Queue size", 0, q2s2.size());

      s1.rollback();

      assertEquals("Queue size", 0, q1s1.size());
      assertEquals("Queue size", 0, q1s2.size());
      assertEquals("Queue size", 1, q2s1.size());
      assertEquals("Queue size", 1, q2s2.size());

    } finally {
      mgr.stop();
      mgr.dispose();
    }
  }

  @Test
  public void testPoll() throws Exception {
    final QueueManager mgr = createQueueManager();

    try {
      mgr.start();

      QueueSession s = mgr.getQueueSession();
      Queue q = s.getQueue("queue1");

      assertEquals("Queue size", 0, q.size());
      Object o = q.poll(0);
      assertEquals("Queue size", 0, q.size());
      assertNull(o);
      o = q.poll(1000);
      assertEquals("Queue size", 0, q.size());
      assertNull(o);
      q.put("String1");
      assertEquals("Queue size", 1, q.size());
      o = q.poll(0);
      assertEquals("Queue size", 0, q.size());
      assertEquals("Queue content", "String1", o);

      final Latch putExecutionLatch = new Latch();
      Thread putExecutionThread = new Thread(() -> {
        try {
          QueueSession s1 = mgr.getQueueSession();
          Queue q1 = s1.getQueue("queue1");
          putExecutionLatch.release();
          q1.put("String1");
        } catch (Exception e) {
          // unlikely to happen. But if it does lets show it in the test logs.
          logger.warn("Error using queue session", e);
        }
      });
      putExecutionThread.start();
      if (!putExecutionLatch.await(THREAD_EXECUTION_TIMEOUT, TimeUnit.MILLISECONDS)) {
        fail("Thread executing put over queue was not executed");
      }
      o = q.poll(RECEIVE_TIMEOUT);
      putExecutionThread.join(THREAD_EXECUTION_TIMEOUT);
      assertEquals("Queue size", q.size(), 0);
      assertEquals("Queue content", "String1", o);

    } finally {
      mgr.stop();
    }
  }

  @Test
  public void testPeek() throws Exception {

    QueueManager mgr = createQueueManager();

    try {
      mgr.start();

      QueueSession s = mgr.getQueueSession();
      Queue q = s.getQueue("queue1");

      assertEquals("Queue size", 0, q.size());
      Object o = q.peek();
      assertEquals("Queue size", 0, q.size());
      assertNull(o);
      q.put("String1");
      assertEquals("Queue size", 1, q.size());
      o = q.peek();
      assertEquals("Queue size", 1, q.size());
      assertEquals("Queue content", "String1", o);
      o = q.poll(1000);
      assertEquals("Queue size", 0, q.size());
      assertEquals("Queue content", "String1", o);

    } finally {
      mgr.stop();
    }
  }

  @Test
  public void testOffer() throws Exception {

    final QueueManager mgr = createQueueManager();
    mgr.setDefaultQueueConfiguration(new DefaultQueueConfiguration(1, false));
    try {
      mgr.start();

      QueueSession s = mgr.getQueueSession();
      Queue q = s.getQueue("queue1");

      assertThat("Queue size", q.size(), is(0));
      assertThat(q.offer("String1", 0L), is(true));
      assertThat("Queue size", q.size(), is(1));
      assertThat(q.offer("String2", 1000), is(false));
      assertThat("Queue size", q.size(), is(1));

      final Latch takeExecutionLatch = new Latch();
      final Thread takeExecutionThread = new Thread(() -> {
        try {
          takeExecutionLatch.release();
          QueueSession s1 = mgr.getQueueSession();
          Queue q1 = s1.getQueue("queue1");
          assertThat("Queue content", q1.take(), is("String1"));
        } catch (Exception e) {
          // unlikely to happen. But if it does lets show it in the test logs.
          logger.warn("Error using queue session", e);
        }
      });
      takeExecutionThread.start();
      if (!takeExecutionLatch.await(THREAD_EXECUTION_TIMEOUT, TimeUnit.MILLISECONDS)) {
        fail("Thread executing put over queue was not executed");
      }
      assertThat(q.offer("String2", 1000), is(true));
      takeExecutionThread.join(THREAD_EXECUTION_TIMEOUT);
      assertThat("Queue size", q.size(), is(1));

    } finally {
      mgr.stop();
    }
  }

  @Test
  public void testRecoverWarmRestart() throws Exception {
    QueueManager mgr = createQueueManager();
    mgr.start();
    QueueSession s = mgr.getQueueSession();
    Queue q = s.getQueue("warmRecoverQueue");

    int toPopulate = 50;

    // Populate queue
    Random rnd = new Random();
    for (int j = 0; j < toPopulate; j++) {
      byte[] o = new byte[2048];
      rnd.nextBytes(o);
      q.put(o);
    }
    assertEquals(q.size(), toPopulate);

    // Stop and start TransactionalQueueManager
    mgr.stop();
    mgr.start();

    assertEquals(toPopulate, q.size());
  }

  @Test
  public void testRecoverColdRestart() throws Exception {
    QueueManager mgr = createQueueManager();
    QueueSession s = mgr.getQueueSession();
    Queue q = s.getQueue("warmRecoverQueue");
    mgr.start();

    int toPopulate = 50;

    // Populate queue
    Random rnd = new Random();
    for (int j = 0; j < toPopulate; j++) {
      byte[] o = new byte[2048];
      rnd.nextBytes(o);
      q.put(o);
    }
    assertEquals(toPopulate, q.size());

    // Stop and recreate TransactionalQueueManager simulating a cold restart
    mgr.stop();
    mgr = createQueueManager();
    s = mgr.getQueueSession();
    q = s.getQueue("warmRecoverQueue");
    mgr.start();
    if (isPersistent()) {
      assertEquals(toPopulate, q.size());
    } else {
      assertEquals(0, q.size());
    }

  }

  @Test
  public void testDisposeQueueWithoutTransaction() throws Exception {
    this.disposeTest.testDisposal(this.createQueueManager(), false);
  }

  @Test
  public void testDisposeQueueInTransaction() throws Exception {
    this.disposeTest.testDisposal(this.createQueueManager(), true);
  }

  @Test
  public void testDisposeQueueByNameInTransaction() throws Exception {
    this.disposeTest.testDisposal(this.createQueueManager(), true);
  }

}
