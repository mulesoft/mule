/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider.CORE_ERROR_TYPE_REPO;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ERROR_METRICS_FACTORY_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_METER_PROVIDER_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_PROFILING_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_DISPATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static org.mule.runtime.core.api.config.MuleProperties.INTERCEPTOR_MANAGER_REGISTRY_KEY;
import static org.mule.runtime.core.internal.processor.rector.profiling.ProfilingTestUtils.mockProcessingStrategyProfilingChainWithoutTriggeringEvent;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.interception.InterceptorManager;
import org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics;
import org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.error.ErrorMetricsFactory;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class QueueManagerLifecycleOrderTestCase extends AbstractMuleContextTestCase {

  private final List<Object> startStopOrder = new ArrayList<>();
  private final RecordingTQM rtqm = new RecordingTQM();

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private MuleContext muleContext;

  @Before
  public void before() throws InitialisationException, ConfigurationException {
    Map<String, Object> objects = new HashMap<>();
    objects.put(FEATURE_FLAGGING_SERVICE_KEY, mock(FeatureFlaggingService.class));
    objects.put(OBJECT_QUEUE_MANAGER, rtqm);
    objects.put(OBJECT_SECURITY_MANAGER, new DefaultMuleSecurityManager());
    objects.put(INTERCEPTOR_MANAGER_REGISTRY_KEY, mock(InterceptorManager.class));
    InternalProfilingService coreProfilingService = mock(InternalProfilingService.class);
    objects.put(MULE_PROFILING_SERVICE_KEY, coreProfilingService);
    objects.put(MULE_ERROR_METRICS_FACTORY_KEY, ErrorMetricsFactory.NO_OP);
    objects.put(MULE_METER_PROVIDER_KEY, MeterProvider.NO_OP);

    mockProcessingStrategyProfilingChainWithoutTriggeringEvent(coreProfilingService);

    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new SimpleConfigurationBuilder(objects));
    testServicesConfigurationBuilder.configure(muleContext);
  }

  @After
  public void after() {
    muleContext.dispose();
  }

  @Test
  public void testStartupOrder() throws Exception {
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(OBJECT_NOTIFICATION_DISPATCHER,
                                                                         mock(NotificationDispatcher.class));
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(ErrorTypeRepository.class.getName(),
                                                                         CORE_ERROR_TYPE_REPO.get());
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(InitialSpanInfoProvider.class.getName(),
                                                                         new DummyComponentTracerFactory());
    FlowConstruct fc = new RecordingFlow("dummy", muleContext);
    fc.setAnnotations(singletonMap(LOCATION_KEY, from("flow")));
    ((MuleContextWithRegistry) muleContext).getRegistry().registerFlowConstruct(fc);
    muleContext.start();
    muleContext.stop();
    assertEquals(4, startStopOrder.size());
    assertSame(rtqm, startStopOrder.get(0));
    assertSame(fc, startStopOrder.get(1));
    assertSame(fc, startStopOrder.get(2));
    assertSame(rtqm, startStopOrder.get(3));

  }

  private class RecordingTQM implements QueueManager {

    @Override
    public void start() throws MuleException {
      startStopOrder.add(this);
    }

    @Override
    public void stop() throws MuleException {
      startStopOrder.add(this);
    }

    @Override
    public QueueSession getQueueSession() {
      throw notImplementedException();
    }

    @Override
    public void setDefaultQueueConfiguration(QueueConfiguration config) {
      throw notImplementedException();
    }

    @Override
    public void setQueueConfiguration(String queueName, QueueConfiguration config) {
      throw notImplementedException();
    }

    @Override
    public Optional<QueueConfiguration> getQueueConfiguration(String queueName) {
      throw notImplementedException();
    }

    private NotImplementedException notImplementedException() {
      return new NotImplementedException("This is test code");
    }
  }

  private class RecordingFlow extends DefaultFlowBuilder.DefaultFlow {

    public RecordingFlow(String name, MuleContext muleContext) {
      super(name, muleContext, null, emptyList(), empty(), empty(), INITIAL_STATE_STARTED, DEFAULT_MAX_CONCURRENCY,
            new DefaultFlowsSummaryStatistics(true), null,
            new ComponentInitialStateManager() {

              @Override
              public boolean mustStartMessageSource(Component component) {
                return true;
              }
            });
    }

    @Override
    public void doStart() throws MuleException {
      super.doStart();
      startStopOrder.add(this);
    }

    @Override
    public void doStop() throws MuleException {
      startStopOrder.add(this);
      super.doStop();
    }
  }
}
