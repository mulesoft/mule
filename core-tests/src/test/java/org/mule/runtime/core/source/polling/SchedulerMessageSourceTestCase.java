/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.polling;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.source.scheduler.SchedulerMessageSource;
import org.mule.runtime.core.source.scheduler.schedule.FixedFrequencyScheduler;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.List;

import org.junit.After;
import org.junit.Test;

public class SchedulerMessageSourceTestCase extends AbstractMuleContextTestCase {

  @Test
  public void emptyStringResponseFromNestedMP() throws Exception {

    SchedulerMessageSource schedulerMessageSource = createMessageSource();

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    schedulerMessageSource.setListener(flow);

    schedulerMessageSource.poll();

    assertNotNull(flow.event);
  }

  @Test
  public void disposeScheduler() throws Exception {
    reset(muleContext.getSchedulerService());
    SchedulerMessageSource schedulerMessageSource = createMessageSource();

    verify(muleContext.getSchedulerService()).ioScheduler();
    List<Scheduler> createdSchedulers = muleContext.getSchedulerService().getSchedulers();
    schedulerMessageSource.start();

    Scheduler pollScheduler = createdSchedulers.get(createdSchedulers.size() - 1);

    verify(pollScheduler).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    schedulerMessageSource.stop();
    schedulerMessageSource.dispose();

    verify(pollScheduler).stop(anyLong(), any());
  }

  private SchedulerMessageSource schedulerMessageSource;

  @After
  public void after() throws MuleException {
    stopIfNeeded(schedulerMessageSource);
    disposeIfNeeded(schedulerMessageSource, logger);
  }

  @Test
  public void setExecutionClassLoader() throws Exception {
    ClassLoader executionClassLoader = mock(ClassLoader.class);
    muleContext.setExecutionClassLoader(executionClassLoader);

    schedulerMessageSource = createMessageSource();

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    schedulerMessageSource.setListener(flow);

    schedulerMessageSource.poll();

    assertNotNull(flow.event);
  }

  private SchedulerMessageSource createMessageSource() throws Exception {
    schedulerMessageSource =
        new SchedulerMessageSource(muleContext, scheduler());
    schedulerMessageSource.setFlowConstruct(getTestFlow(muleContext));
    schedulerMessageSource.initialise();
    return schedulerMessageSource;
  }

  private FixedFrequencyScheduler scheduler() {
    FixedFrequencyScheduler factory = new FixedFrequencyScheduler();
    factory.setFrequency(1000);
    return factory;
  }

}
