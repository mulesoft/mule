/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.polling;

import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.LOCATION_KEY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.internal.source.scheduler.DefaultSchedulerMessageSource;
import org.mule.runtime.core.api.source.polling.FixedFrequencyScheduler;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;

public class DefaultSchedulerMessageSourceTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = getLogger(DefaultSchedulerMessageSourceTestCase.class);

  @Test
  public void simplePoll() throws Exception {

    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    schedulerMessageSource.setListener(flow);
    schedulerMessageSource.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));

    schedulerMessageSource.trigger();
    new PollingProber(RECEIVE_TIMEOUT, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return flow.event != null;
      }

      @Override
      public String describeFailure() {
        return "flow event never set by the source flow";
      }
    });
  }

  @Test
  public void disposeScheduler() throws Exception {
    reset(muleContext.getSchedulerService());
    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();

    verify(muleContext.getSchedulerService()).cpuLightScheduler();
    List<Scheduler> createdSchedulers = muleContext.getSchedulerService().getSchedulers();
    schedulerMessageSource.start();

    Scheduler pollScheduler = createdSchedulers.get(createdSchedulers.size() - 1);

    verify(pollScheduler).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    schedulerMessageSource.stop();
    schedulerMessageSource.dispose();

    verify(pollScheduler).stop();
  }

  private DefaultSchedulerMessageSource schedulerMessageSource;

  @After
  public void after() throws MuleException {
    stopIfNeeded(schedulerMessageSource);
    disposeIfNeeded(schedulerMessageSource, LOGGER);
  }

  private DefaultSchedulerMessageSource createMessageSource() throws Exception {
    schedulerMessageSource =
        new DefaultSchedulerMessageSource(muleContext, scheduler());
    schedulerMessageSource.setAnnotations(getAppleFlowComponentLocationAnnotations());
    schedulerMessageSource.initialise();
    return schedulerMessageSource;
  }

  private FixedFrequencyScheduler scheduler() {
    FixedFrequencyScheduler factory = new FixedFrequencyScheduler();
    factory.setFrequency(1000);
    return factory;
  }

}
