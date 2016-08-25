/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.concurrency;

import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

/**
 * This class is very useful for test cases which need to make assertions concurrently in different threads. Usage is as follows:
 * <code>
 *   public void testConcurrency() 
 *   {                
 *       TestingThread thread = new TestingThread()
 *       {
 *           protected void doRun() throws Throwable
 *           {
 *               // Wait a few seconds for somethingElse to begin
 *               Thread.sleep(3000);
 *               assertTrue(somethingElse.isRunning());
 *               assertEquals(3, somethingElse.counter);
 *           }
 *       };
 *       thread.start();
 *
 *       // This will block the main test thread
 *       runSomething("big long task");
 *       assertEquals("peachy", something.getResult());
 *       assertFalse(something.isOutOfControl());
 *       
 *       // Verify that no exceptions occurred meanwhile in the TestingThread
 *       thread.await();
 *       if (thread.getException() != null)
 *       {
 *           fail(thread.getException().getResult());
 *       }
 *   }
 * </code> Both the TestingThread and the main thread will run in parallel, therefore assertions can be made on "somethingElse"
 * while the call to runSomething() is blocking the main thread of the TestCase.
 */
public abstract class TestingThread extends Thread implements Runnable {

  public static final long AWAIT_TIMEOUT = 10000;
  private static final long awaitTimeout = Long.getLong("mule.test.timeoutSecs", AWAIT_TIMEOUT / 1000L) * 1000L;
  private final Latch done = new Latch();
  private volatile Throwable exception = null;

  /**
   * Executes the doRun() method and stores any exception which occurred to be returned at a later time.
   */
  @Override
  public final void run() {
    try {
      doRun();
    } catch (Throwable e) {
      exception = e;
    } finally {
      done.countDown();
    }
  }

  abstract protected void doRun() throws Throwable;

  /**
   * Block until the thread completes its doRun() method.
   * 
   * @throws InterruptedException
   */
  public void await() throws InterruptedException {
    done.await(awaitTimeout, TimeUnit.MILLISECONDS);
  }

  /**
   * @return any exception thrown by the doRun() method, including failed assertions
   */
  public Throwable getException() {
    return exception;
  }
}
