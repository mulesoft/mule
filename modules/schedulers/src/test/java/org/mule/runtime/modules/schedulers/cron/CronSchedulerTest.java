/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.modules.schedulers.cron;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.mule.runtime.core.api.MuleException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.runtime.core.source.polling.PollingTask;
import org.mule.runtime.core.source.polling.PollingWorker;

import java.util.TimeZone;

import org.junit.Test;


public class CronSchedulerTest extends AbstractMuleContextTestCase {

  private Prober pollingProber = new PollingProber(1000, 0l);

  @Test
  public void validateLifecycleHappyPath() throws MuleException {
    CronScheduler scheduler = createVoidScheduler();

    scheduler.initialise();
    scheduler.start();
    scheduler.stop();
    scheduler.dispose();
  }

  @Test
  public void stopAfterInitializeShouldNotFail() throws MuleException {
    CronScheduler scheduler = createVoidScheduler();
    scheduler.initialise();
    scheduler.stop();
  }


  @Test
  public void startAfterStopShouldNotFail() throws Exception {
    PollingWorker mockPollingWorker = mock(PollingWorker.class);
    CronScheduler scheduler = createScheduler(mockPollingWorker);

    scheduler.initialise();
    scheduler.start();
    scheduler.stop();

    reset(mockPollingWorker);

    verify(mockPollingWorker, never()).run();

    scheduler.start();
    scheduler.schedule();

    pollingProber.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        try {
          verify(mockPollingWorker, atLeastOnce()).run();
          return true;
        } catch (Exception e) {
          throw new RuntimeException("unexpected exception from mock task");
        } catch (AssertionError e) {
          return false;
        }
      }

      @Override
      public String describeFailure() {
        return "The scheduler was never run";
      }
    });
  }

  private CronScheduler createVoidScheduler() {
    CronScheduler scheduler =
        new CronScheduler("name", new PollingWorker(mock(PollingTask.class), muleContext.getExceptionListener()), "0/1 * * * * ?",
                          TimeZone.getDefault());
    scheduler.setMuleContext(muleContext);
    return scheduler;
  }

  private CronScheduler createScheduler(PollingWorker worker) {
    CronScheduler cronScheduler = new CronScheduler("name", worker, "0/1 * * * * ?", TimeZone.getDefault());
    cronScheduler.setMuleContext(muleContext);
    return cronScheduler;
  }

}
