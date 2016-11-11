/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.source.polling.schedule;


import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.compatibility.core.transport.AbstractPollingMessageReceiver;
import org.mule.compatibility.core.transport.PollingReceiverWorker;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencyScheduledPollFactory;
import org.mule.runtime.core.source.polling.schedule.ScheduledPoll;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

public class FixedFrequencyScheduledPollFactoryTestCase extends AbstractMuleTestCase {

  private MuleContext muleContext = mockContextWithServices();

  private AbstractPollingMessageReceiver receiver = mock(AbstractPollingMessageReceiver.class);

  private SimpleUnitTestSupportSchedulerService schedulerService;

  @Before
  public void before() {
    schedulerService = (SimpleUnitTestSupportSchedulerService) muleContext.getSchedulerService();
  }

  @Test
  public void testCreatesCorrectInstance() throws MuleException {
    FixedFrequencyScheduledPollFactory factory = new FixedFrequencyScheduledPollFactory();
    factory.setMuleContext(muleContext);
    factory.setFrequency(300);
    factory.setStartDelay(400);
    factory.setTimeUnit(DAYS);

    PollingReceiverWorker worker = new PollingReceiverWorker(receiver);
    ScheduledPoll scheduler = factory.doCreate("name", worker);

    scheduler.initialise();
    scheduler.start();

    verify(schedulerService).ioScheduler();
    Scheduler createdScheduler = schedulerService.getCreatedSchedulers().get(0);
    verify(createdScheduler).scheduleAtFixedRate(any(), eq(400l), eq(300l), eq(DAYS));

    assertThat(scheduler.getName(), is("name"));
  }

  @Test
  public void testDefaultValues() throws MuleException {
    FixedFrequencyScheduledPollFactory factory = new FixedFrequencyScheduledPollFactory();
    factory.setMuleContext(muleContext);

    PollingReceiverWorker worker = new PollingReceiverWorker(receiver);
    ScheduledPoll scheduler = factory.doCreate("name", worker);

    scheduler.initialise();
    scheduler.start();

    verify(schedulerService).ioScheduler();
    Scheduler createdScheduler = schedulerService.getCreatedSchedulers().get(0);
    verify(createdScheduler).scheduleAtFixedRate(any(), eq(1000l), eq(1000l), eq(MILLISECONDS));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeFrequency() {
    FixedFrequencyScheduledPollFactory factory = new FixedFrequencyScheduledPollFactory();
    factory.setFrequency(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeStartDelay() {
    FixedFrequencyScheduledPollFactory factory = new FixedFrequencyScheduledPollFactory();
    factory.setStartDelay(-1);
  }
}
