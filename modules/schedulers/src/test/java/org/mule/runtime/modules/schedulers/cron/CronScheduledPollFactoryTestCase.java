/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.modules.schedulers.cron;

import static java.util.TimeZone.getDefault;
import static java.util.TimeZone.getTimeZone;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.source.polling.schedule.ScheduledPoll;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

public class CronScheduledPollFactoryTestCase extends AbstractMuleTestCase {

  private MuleContext muleContext = mockContextWithServices();

  private SimpleUnitTestSupportSchedulerService schedulerService;

  @Before
  public void before() {
    schedulerService = (SimpleUnitTestSupportSchedulerService) muleContext.getSchedulerService();
  }

  @Test
  public void testSchedulerCreation() throws MuleException {
    CronScheduledPollFactory factory = new CronScheduledPollFactory();
    factory.setMuleContext(muleContext);
    factory.setExpression("0 * * * ? *");

    ScheduledPoll scheduler = factory.create("name", () -> {
    });

    scheduler.initialise();
    scheduler.start();

    verify(schedulerService).ioScheduler();
    Scheduler createdScheduler = schedulerService.getCreatedSchedulers().get(0);
    verify(createdScheduler).scheduleWithCronExpression(any(), eq("0 * * * ? *"), eq(getDefault()));
  }

  @Test
  public void testSchedulerCreationWithTimeZone() throws MuleException {
    CronScheduledPollFactory factory = new CronScheduledPollFactory();
    factory.setMuleContext(muleContext);
    factory.setExpression("0 * * * ? *");
    factory.setTimeZone(getTimeZone("America/Argentina/Buenos_Aires").getID());

    ScheduledPoll scheduler =
        factory.create("name", () -> {
        });

    scheduler.initialise();
    scheduler.start();

    verify(schedulerService).ioScheduler();
    Scheduler createdScheduler = schedulerService.getCreatedSchedulers().get(0);
    verify(createdScheduler).scheduleWithCronExpression(any(), eq("0 * * * ? *"),
                                                        eq(getTimeZone("America/Argentina/Buenos_Aires")));
  }

  @Test
  public void testSchedulerCreationWithAnotherTimeZone() throws MuleException {
    CronScheduledPollFactory factory = new CronScheduledPollFactory();
    factory.setMuleContext(muleContext);
    factory.setExpression("0 * * * ? *");
    factory.setTimeZone(getTimeZone("Europe/London").getID());

    ScheduledPoll scheduler =
        factory.create("name", () -> {
        });

    scheduler.initialise();
    scheduler.start();

    verify(schedulerService).ioScheduler();
    Scheduler createdScheduler = schedulerService.getCreatedSchedulers().get(0);
    verify(createdScheduler).scheduleWithCronExpression(any(), eq("0 * * * ? *"), eq(getTimeZone("Europe/London")));
  }

}
