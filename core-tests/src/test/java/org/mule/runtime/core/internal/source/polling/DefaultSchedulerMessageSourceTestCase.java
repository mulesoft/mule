/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.polling;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY;
import static org.mule.tck.MuleTestUtils.createFlowWithSource;
import static org.slf4j.LoggerFactory.getLogger;

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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

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
    return of(REGISTRY_KEY, componentLocator,
              OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY, createDefaultErrorTypeRepository());
  }

  @Test
  public void simplePoll() throws Exception {

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

}
