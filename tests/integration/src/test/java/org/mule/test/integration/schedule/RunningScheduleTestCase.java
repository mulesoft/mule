/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.exception.MuleException;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

/**
 * This test checks that a Scheduler can be stopped, executed and started. Also shows how a customer can set his own scheduler in
 * mule config.
 *
 * It also shows the way users can add a new Scheduler as a spring bean.
 */
public class RunningScheduleTestCase extends AbstractIntegrationTestCase {

  public static final String SCHEDULER_NAME = "testScheduler";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/schedule/scheduler-config.xml";
  }

  @Test
  public void test() throws Exception {
    MockScheduler scheduler = findScheduler(SCHEDULER_NAME);
    new PollingProber(2000, 50).check(new JUnitLambdaProbe(() -> {
      assertTrue(scheduler.getCount() > 0);
      return true;
    }));

    stopSchedulers();

    Thread.sleep(2000);

    int count = scheduler.getCount();

    Thread.sleep(2000);

    assertEquals(count, scheduler.getCount());
  }

  private void stopSchedulers() throws MuleException {
    findScheduler(SCHEDULER_NAME).stop();
  }

  private MockScheduler findScheduler(String schedulerName) {
    return (MockScheduler) muleContext.getRegistry().lookupObject(schedulerName);
  }
}
