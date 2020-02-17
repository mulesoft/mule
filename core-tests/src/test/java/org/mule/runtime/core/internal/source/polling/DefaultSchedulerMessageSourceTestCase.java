/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.polling;

import static java.lang.System.setProperty;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_SCHEDULERS;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createFlowWithSource;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.execution.MuleMessageProcessingManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.policy.SourcePolicy;
import org.mule.runtime.core.internal.source.scheduler.DefaultSchedulerMessageSource;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.slf4j.Logger;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(SCHEDULER)
public class DefaultSchedulerMessageSourceTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = getLogger(DefaultSchedulerMessageSourceTestCase.class);
  private PolicyManager policyManager;
  private SourcePolicy sourcePolicy;
  private final String MESSAGE_PROCESSING_MANAGER_KEY = "_muleMessageProcessingManager";

  @Before
  public void setUp() throws Exception {
    policyManager = mock(PolicyManager.class);
    sourcePolicy = mock(SourcePolicy.class);
    when(policyManager.createSourcePolicyInstance(any(), any(), any(), any())).thenReturn(sourcePolicy);

    MuleMessageProcessingManager processingManager = new MuleMessageProcessingManager();
    processingManager.setMuleContext(muleContext);
    processingManager.setPolicyManager(policyManager);

    ((DefaultMuleContext) muleContext).getRegistry().unregisterObject(MESSAGE_PROCESSING_MANAGER_KEY);
    ((DefaultMuleContext) muleContext).getRegistry().registerObject(MESSAGE_PROCESSING_MANAGER_KEY, processingManager);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @Test
  public void simplePoll() throws Exception {
    pollAndAssertWith(p -> new PollingProber(RECEIVE_TIMEOUT, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return p.event != null;
      }

      @Override
      public String describeFailure() {
        return "flow event never set by the source flow";
      }
    }));
  }

  @Test
  @Description("Verifies that no events are processed when the scheduler is disabled")
  @Issue("MULE-18063")
  public void simplePollDisabledWithScheduler() throws Exception {
    String wasDisabled = setProperty(MULE_DISABLE_SCHEDULERS, "true");
    try {
      pollAndAssertWith(p -> new PollingProber(RECEIVE_TIMEOUT, 100).check(new Probe() {

        @Override
        public boolean isSatisfied() {
          return p.event == null;
        }

        @Override
        public String describeFailure() {
          return "unexpected event when scheduler is disabled";
        }
      }));
    } finally {
      setProperty(MULE_DISABLE_SCHEDULERS, wasDisabled != null ? wasDisabled : "false");
    }
  }

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

    String wasDisabled = setProperty(MULE_DISABLE_SCHEDULERS, "true");

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

  private DefaultSchedulerMessageSource schedulerMessageSource;

  @After
  public void after() throws MuleException {
    stopIfNeeded(schedulerMessageSource);
    disposeIfNeeded(schedulerMessageSource, LOGGER);
  }

  private DefaultSchedulerMessageSource createMessageSource() throws Exception {
    schedulerMessageSource =
        new DefaultSchedulerMessageSource(muleContext, scheduler(), false);
    schedulerMessageSource.setAnnotations(getAppleFlowComponentLocationAnnotations());

    // Manually create and register flow
    Flow flow = createFlowWithSource(muleContext, APPLE_FLOW, schedulerMessageSource);
    when(componentLocator.find(Location.builder().globalName(APPLE_FLOW).build())).thenReturn(of(flow));
    // scheduler source is initialized when it's registered as the flow's source in the registry
    ((DefaultMuleContext) muleContext).getRegistry().registerFlowConstruct(flow);

    // Injecting processing manager dependency
    muleContext.getInjector().inject(schedulerMessageSource);
    return schedulerMessageSource;
  }

  private FixedFrequencyScheduler scheduler() {
    FixedFrequencyScheduler factory = new FixedFrequencyScheduler();
    factory.setFrequency(1000);
    return factory;
  }

  private void pollAndAssertWith(Consumer<SensingNullMessageProcessor> validate) throws Exception {
    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    schedulerMessageSource.setListener(flow);
    schedulerMessageSource.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));

    doAnswer(invocationOnMock -> {
      CoreEvent inputEvent = invocationOnMock.getArgument(0);
      flow.process(inputEvent);
      return null;
    }).when(sourcePolicy).process(any(CoreEvent.class), any(), any());

    schedulerMessageSource.trigger();

    // let some time before making the assertion to avoid assert before the scheduler start
    MILLISECONDS.sleep(100);

    validate.accept(flow);
  }

  public SchedulerService mockSchedulerService(AtomicReference<Scheduler> pollScheduler) throws Exception {
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
