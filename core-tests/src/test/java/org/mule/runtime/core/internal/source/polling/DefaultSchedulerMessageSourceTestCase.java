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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.core.internal.source.scheduler.DefaultSchedulerMessageSource;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultSchedulerMessageSourceTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = getLogger(DefaultSchedulerMessageSourceTestCase.class);

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

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
    SchedulerService schedulerService = muleContext.getSchedulerService();
    reset(schedulerService);

    AtomicReference<Scheduler> pollScheduler = new AtomicReference<>();

    doAnswer(invocation -> {
      Scheduler scheduler = (Scheduler) invocation.callRealMethod();
      pollScheduler.set(scheduler);
      return scheduler;
    }).when(schedulerService).cpuLightScheduler();

    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();
    verify(schedulerService).cpuLightScheduler();

    schedulerMessageSource.start();

    verify(pollScheduler.get()).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    schedulerMessageSource.stop();
    schedulerMessageSource.dispose();

    verify(pollScheduler.get()).stop();
  }

  private DefaultSchedulerMessageSource schedulerMessageSource;

  @After
  public void after() throws MuleException {
    stopIfNeeded(schedulerMessageSource);
    disposeIfNeeded(schedulerMessageSource, LOGGER);
  }

  private DefaultSchedulerMessageSource createMessageSource() throws Exception {
    createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
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
