/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.scheduler;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_SCHEDULERS;
import static org.mule.runtime.core.internal.source.scheduler.DefaultSchedulerMessageSource.setDisabled;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(SCHEDULER)
public class DefaultSchedulerMessageSourceDisposeTestCase extends DefaultSchedulerMessageSourceTestCase {

  @Test
  public void disposeScheduler() throws Exception {
    AtomicReference<Scheduler> pollScheduler = new AtomicReference<>();

    SchedulerService schedulerService = mockSchedulerService(pollScheduler);

    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();

    verify(schedulerService).cpuLightScheduler();

    schedulerMessageSource.start();

    verify(pollScheduler.get()).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    schedulerMessageSource.stop();
    schedulerMessageSource.dispose();

    verify(pollScheduler.get()).stop();
  }

  @Test
  @Description("Verifies that a disabled scheduler is neither initialized nor disposed")
  @Issue("MULE-18063")
  public void disposeDisabledScheduler() throws Exception {

    String wasDisabled = getProperty(MULE_DISABLE_SCHEDULERS);

    //noinspection deprecation
    setDisabled(Boolean.valueOf("true"));

    try {
      AtomicReference<Scheduler> pollScheduler = new AtomicReference<>();

      SchedulerService schedulerService = mockSchedulerService(pollScheduler);

      DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();

      verify(schedulerService, never()).cpuLightScheduler();

      schedulerMessageSource.start();

      assertThat(pollScheduler.get(), is(nullValue()));

      schedulerMessageSource.stop();
      schedulerMessageSource.dispose();

      assertThat(pollScheduler.get(), is(nullValue()));
    } finally {
      setProperty(MULE_DISABLE_SCHEDULERS, wasDisabled != null ? wasDisabled : "false");
    }
  }

  private SchedulerService mockSchedulerService(AtomicReference<Scheduler> pollScheduler) throws Exception {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    reset(schedulerService);

    doAnswer(invocation -> {
      Scheduler scheduler = (Scheduler) invocation.callRealMethod();
      pollScheduler.set(scheduler);
      return scheduler;
    }).when(schedulerService).cpuLightScheduler();

    return schedulerService;
  }
}
