/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

@SmallTest
public class NamedThreadFactoryTestCase extends AbstractMuleTestCase {

  protected Latch latch = new Latch();
  protected String testThreadName = "myThread";
  protected ClassLoader testClassLoader = new ClassLoader() {};
  protected Runnable nullRunnable = () -> {
  };

  @Test
  public void testNameContextClassloader() throws InterruptedException {
    NamedThreadFactory threadFactory = new NamedThreadFactory(testThreadName, testClassLoader);
    Thread t = threadFactory.newThread(() -> {
      assertEquals(testThreadName + ".01", Thread.currentThread().getName());
      assertEquals(testClassLoader, Thread.currentThread().getContextClassLoader());
      latch.countDown();
    });
    t.start();
    assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testNameIncrement() throws InterruptedException {
    NamedThreadFactory threadFactory = new NamedThreadFactory(testThreadName);
    Thread t = threadFactory.newThread(nullRunnable);
    assertEquals(testThreadName + ".01", t.getName());
    t = threadFactory.newThread(nullRunnable);
    assertEquals(testThreadName + ".02", t.getName());
    t = threadFactory.newThread(nullRunnable);
    assertEquals(testThreadName + ".03", t.getName());
  }

}
