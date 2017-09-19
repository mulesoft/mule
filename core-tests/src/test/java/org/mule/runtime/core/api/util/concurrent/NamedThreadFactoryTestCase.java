/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
