/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.polling;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLICY_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createFlowWithSource;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SchedulerStories.SCHEDULED_FLOW_EXECUTION;

import static java.util.Collections.singletonMap;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
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

import io.qameta.allure.Issue;
import org.slf4j.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SCHEDULER)
@Story(SCHEDULED_FLOW_EXECUTION)
public class DefaultSchedulerMessageSourceTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = getLogger(DefaultSchedulerMessageSourceTestCase.class);
  private Flow flow;
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

    ((DefaultMuleContext) muleContext).getRegistry().unregisterObject(OBJECT_POLICY_MANAGER);
    ((DefaultMuleContext) muleContext).getRegistry().registerObject(OBJECT_POLICY_MANAGER, policyManager);
    ((DefaultMuleContext) muleContext).getRegistry().unregisterObject(MESSAGE_PROCESSING_MANAGER_KEY);
    ((DefaultMuleContext) muleContext).getRegistry().registerObject(MESSAGE_PROCESSING_MANAGER_KEY, processingManager);
  }

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
  @Issue("W-17981248")
  public void testStartWhenStopping() throws Exception {
    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();
    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    schedulerMessageSource.setListener(flow);
    schedulerMessageSource.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));

    DefaultMuleContext spyMuleContext = spy((DefaultMuleContext) muleContext);
    doReturn(true).when(spyMuleContext).isStopping();

    schedulerMessageSource.setMuleContext(spyMuleContext);
    schedulerMessageSource.start();

    assertThat("Scheduler should not be started when MuleContext is stopping",
               schedulerMessageSource.isStarted(), is(false));
  }

  @Test
  @Issue("W-17981248")
  public void testRunWhenStopping() throws Exception {
    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();
    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    schedulerMessageSource.setListener(flow);
    schedulerMessageSource.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));

    DefaultMuleContext spyMuleContext = spy((DefaultMuleContext) muleContext);
    doReturn(false).when(spyMuleContext).isStopping();
    doReturn(true).when(spyMuleContext).isPrimaryPollingInstance();

    schedulerMessageSource.setMuleContext(spyMuleContext);
    schedulerMessageSource.start();
    doReturn(true).when(spyMuleContext).isStopping();

    verify(spyMuleContext, atLeastOnce()).isStopping();
    // isPrimaryPollingInstance() was never called if isStopping() returns true
    verify(spyMuleContext, never()).isPrimaryPollingInstance();
    assertThat("Flow should not be executed when MuleContext is stopping", flow.event, is(nullValue()));
  }

  @Test
  @Issue("W-17981248")
  public void testPollWhenStopping() throws Exception {
    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();
    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    schedulerMessageSource.setListener(flow);
    schedulerMessageSource.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));

    DefaultMuleContext spyMuleContext = spy((DefaultMuleContext) muleContext);
    doReturn(true).when(spyMuleContext).isStopping();

    schedulerMessageSource.setMuleContext(spyMuleContext);
    schedulerMessageSource.trigger();
    Thread.sleep(100);

    verify(spyMuleContext, times(1)).isStopping();
    assertThat("Flow should not be executed when MuleContext is stopping", flow.event, is(nullValue()));
  }

  @Test
  public void disposeScheduler() throws Exception {
    SchedulerService schedulerService = muleContext.getSchedulerService();
    reset(schedulerService);

    AtomicReference<Scheduler> pollScheduler = new AtomicReference<>();

    doAnswer(invocation -> {
      if (pollScheduler.get() == null) {
        Scheduler scheduler = (Scheduler) invocation.callRealMethod();
        final Scheduler spiedScheduler = spy(scheduler);
        doAnswer(inv -> {
          scheduler.stop();
          return null;
        }).when(spiedScheduler).stop();
        pollScheduler.set(spiedScheduler);
      }
      return pollScheduler.get();
    }).when(schedulerService).cpuLightScheduler(any(SchedulerConfig.class));

    DefaultSchedulerMessageSource schedulerMessageSource = createMessageSource();
    final ArgumentCaptor<SchedulerConfig> schedulerConfigCaptor = forClass(SchedulerConfig.class);
    verify(schedulerService, atLeastOnce()).cpuLightScheduler(schedulerConfigCaptor.capture());
    assertThat(schedulerConfigCaptor.getValue().getSchedulerName(),
               is("DefaultMessageProcessorChain '(chain) of 'appleFlow' processor chain' .switchOnErrorScheduler"));

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

    stopIfNeeded(flow);
    disposeIfNeeded(flow, LOGGER);
  }

  private DefaultSchedulerMessageSource createMessageSource() throws Exception {
    schedulerMessageSource = new DefaultSchedulerMessageSource(scheduler(), false);
    schedulerMessageSource.setAnnotations(getAppleFlowComponentLocationAnnotations());

    // Manually create and register flow
    flow = createFlowWithSource(muleContext, APPLE_FLOW, schedulerMessageSource);
    when(componentLocator.find(Location.builder().globalName(APPLE_FLOW).build())).thenReturn(of(flow));
    // scheduler source is initialized when it's registered as the flow's source in the registry
    ((DefaultMuleContext) muleContext).getRegistry().registerFlowConstruct(flow);

    startIfNeeded(flow);

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
