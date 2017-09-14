/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.monitor;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.privileged.util.monitor.Expirable;
import org.mule.runtime.core.privileged.util.monitor.ExpiryMonitor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExpiryMonitorTestCase extends AbstractMuleContextTestCase {

  private static final int EXPIRE_TIME = 300;
  private static final int EXPIRE_INTERVAL = 100;
  // Add some time to account for the durations of the expiry process, since it is scheduled with fixed delay
  private static final int EXPIRE_TIMEOUT = EXPIRE_TIME + EXPIRE_INTERVAL + 200;
  private static final long DELTA_TIME = 10;

  private volatile boolean expired = false;
  private volatile long expiredTime = -1;

  private ExpiryMonitor monitor;

  @Before
  public void before() throws Exception {
    expired = false;
    monitor = new ExpiryMonitor("test", EXPIRE_INTERVAL, muleContext, false);
  }

  @After
  public void after() {
    monitor.dispose();
  }

  @Test
  public void testExpiry() throws InterruptedException {
    Expirable e = () -> expire();
    monitor.addExpirable(EXPIRE_TIME, MILLISECONDS, e);

    new PollingProber(EXPIRE_TIMEOUT, 50).check(new JUnitLambdaProbe(() -> {
      assertThat(monitor.isRegistered(e), is(false));
      assertThat(expired, is(true));
      return true;
    }, ae -> {
      ae.printStackTrace();
      return "" + currentTimeMillis() + " - " + monitor.toString();
    }));
  }

  @Test
  public void testNotExpiry() throws InterruptedException {
    Expirable e = () -> expire();
    long startTime = currentTimeMillis();
    monitor.addExpirable(EXPIRE_TIME, MILLISECONDS, e);
    monitor.run();
    assertThat(expired, is(false));

    new PollingProber(EXPIRE_TIMEOUT, 50).check(new JUnitLambdaProbe(() -> {
      assertThat(monitor.isRegistered(e), is(false));
      assertThat(expired, is(true));
      return true;
    }, ae -> {
      ae.printStackTrace();
      return "" + currentTimeMillis() + " - " + monitor.toString();
    }));
    assertThat(expiredTime - startTime, greaterThanOrEqualTo(EXPIRE_TIME - DELTA_TIME));
  }

  @Test
  public void testExpiryWithReset() throws InterruptedException {
    Expirable e = () -> expire();
    monitor.addExpirable(EXPIRE_TIME, MILLISECONDS, e);
    monitor.run();
    assertThat(expired, is(false));
    long startTime = currentTimeMillis();
    monitor.resetExpirable(e);
    monitor.run();
    assertTrue(!expired);

    new PollingProber(EXPIRE_TIMEOUT, 50).check(new JUnitLambdaProbe(() -> {
      assertThat(monitor.isRegistered(e), is(false));
      assertThat(expired, is(true));
      return true;
    }, ae -> {
      ae.printStackTrace();
      return "" + currentTimeMillis() + " - " + monitor.toString();
    }));
    assertThat(expiredTime - startTime, greaterThanOrEqualTo(EXPIRE_TIME - DELTA_TIME));
  }

  @Test
  public void testNotExpiryWithRemove() throws InterruptedException {
    Expirable e = () -> expire();
    monitor.addExpirable(EXPIRE_TIME, MILLISECONDS, e);
    monitor.run();
    assertThat(expired, is(false));
    monitor.removeExpirable(e);

    Thread.sleep(EXPIRE_TIMEOUT);
    assertThat(monitor.isRegistered(e), is(false));
    assertThat(expired, is(false));
  }

  private void expire() {
    expiredTime = currentTimeMillis();
    expired = true;
  }

}
