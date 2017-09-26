/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class RegisteredObjectLifecycleTestCase extends AbstractMuleContextTestCase {

  private static final long TIMEOUT = 200;

  protected Latch initLatch;
  protected Latch startLatch;
  protected Latch stopLatch;
  protected Latch disposeLatch;

  private DummyBean bean = new DummyBean();

  @Override
  protected void doSetUp() throws Exception {
    bean = new DummyBean();
    initLatch = new Latch();
    startLatch = new Latch();
    stopLatch = new Latch();
    disposeLatch = new Latch();
  }

  @Test
  public void testLifecycleForMuleContext() throws Exception {
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject("dummy", bean);
    assertTrue(initLatch.await(TIMEOUT, MILLISECONDS));
    Thread.sleep(TIMEOUT);
    assertThat(startLatch.getCount(), is(1L));
    assertThat(stopLatch.getCount(), is(1L));
    assertThat(disposeLatch.getCount(), is(1L));

    muleContext.start();
    assertTrue(startLatch.await(TIMEOUT, MILLISECONDS));
    Thread.sleep(TIMEOUT);
    assertThat(stopLatch.getCount(), is(1L));
    assertThat(disposeLatch.getCount(), is(1L));

    muleContext.stop();
    assertTrue(stopLatch.await(TIMEOUT, MILLISECONDS));
    Thread.sleep(TIMEOUT);
    assertThat(disposeLatch.getCount(), is(1L));

    muleContext.dispose();
    assertTrue(disposeLatch.await(TIMEOUT, MILLISECONDS));
  }

  @Test
  public void testLifecycleForUnregisteredObject() throws Exception {
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject("dummy", bean);
    assertTrue(initLatch.await(TIMEOUT, MILLISECONDS));
    Thread.sleep(TIMEOUT);
    assertThat(startLatch.getCount(), is(1L));
    assertThat(stopLatch.getCount(), is(1L));
    assertThat(disposeLatch.getCount(), is(1L));

    muleContext.start();
    assertTrue(startLatch.await(TIMEOUT, MILLISECONDS));
    Thread.sleep(TIMEOUT);
    assertThat(stopLatch.getCount(), is(1L));
    assertThat(disposeLatch.getCount(), is(1L));

    ((MuleContextWithRegistries) muleContext).getRegistry().unregisterObject("dummy");
    assertTrue(stopLatch.await(TIMEOUT, MILLISECONDS));
    assertTrue(disposeLatch.await(TIMEOUT, MILLISECONDS));
  }

  public class DummyBean implements Lifecycle {

    public String echo(String echo) {
      return echo;
    }

    @Override
    public void initialise() throws InitialisationException {
      initLatch.countDown();
    }

    @Override
    public void start() throws MuleException {
      startLatch.countDown();
    }

    @Override
    public void stop() throws MuleException {
      stopLatch.countDown();
    }

    @Override
    public void dispose() {
      disposeLatch.countDown();
    }

    public boolean isInitialised() {
      return false;
    }

    public boolean isStarted() {
      return false;
    }

    public boolean isStopped() {
      return false;
    }

    public boolean isDisposed() {
      return false;
    }
  }
}
