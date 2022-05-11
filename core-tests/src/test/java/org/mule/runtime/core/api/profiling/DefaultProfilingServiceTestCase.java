/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.profiling;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_PROFILING_SERVICE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.EXTENSION_PROFILING_EVENT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_DISPATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_HANDLER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.processor.rector.profiling.ProfilingTestUtils.enableProfilingFeatureTestConsumer;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

import static java.util.Collections.singleton;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.ExtensionProfilingEventContext;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.DefaultProfilingNotificationListener;
import org.mule.runtime.core.internal.profiling.ResettableProfilingDataProducerDelegate;
import org.mule.runtime.core.internal.profiling.consumer.LoggerByteBufferAllocationProfilingDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.LoggerComponentThreadingDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.TaskSchedulingLoggerDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.TransactionLoggerDataConsumer;
import org.mule.runtime.core.internal.profiling.discovery.CompositeProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.core.internal.profiling.notification.ProfilingNotification;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Feature(PROFILING)
@Story(DEFAULT_PROFILING_SERVICE)
public class DefaultProfilingServiceTestCase extends AbstractMuleContextTestCase {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  private final ProfilingDataConsumer<TestProfilingEventContext> testProfilingDataConsumer = new TestProfilingDataConsumer();

  @Mock
  private ServerNotificationManager notificationManager;

  @Mock
  protected FeatureFlaggingService featureFlaggingService;

  private DefaultProfilingService profilingService;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();
    objects.put(OBJECT_NOTIFICATION_DISPATCHER, notificationManager);
    objects.put(OBJECT_NOTIFICATION_HANDLER, notificationManager);
    return objects;
  }

  @Before
  public void configureProfilingService() throws MuleException {
    profilingService = new DefaultProfilingService();
    initialiseIfNeeded(profilingService, muleContext);
    profilingService
        .setProfilingDataConsumerDiscoveryStrategies(of(singleton(new TestProfilingDataConsumerDiscoveryStrategy())));
    when(featureFlaggingService.isEnabled(ENABLE_PROFILING_SERVICE)).thenReturn(true);
    startIfNeeded(profilingService);
    profilingService.registerProfilingDataProducer(TestProfilingEventType.TEST_PROFILING_EVENT_TYPE,
                                                   new TestProfilingDataProducer(profilingService));
    setTestFeatureStatus(true);
  }

  @After
  public void after() throws MuleException {
    setTestFeatureStatus(false);
  }

  private void setTestFeatureStatus(boolean status) throws RegistrationException {
    enableProfilingFeatureTestConsumer(muleContext, EXTENSION_PROFILING_EVENT, status);
    enableProfilingFeatureTestConsumer(muleContext, STARTING_OPERATION_EXECUTION, status);
  }

  @Test
  @Description("When profiling data consumers are obtained, the correct data producers are returned")
  public void correctDataProducersObtained() {
    ProfilingDataProducer<TestProfilingEventContext, Object> profilingDataProducer =
        profilingService.getProfilingDataProducer(TestProfilingEventType.TEST_PROFILING_EVENT_TYPE);
    assertThat(profilingDataProducer, instanceOf(ResettableProfilingDataProducerDelegate.class));
    assertThat(((ResettableProfilingDataProducerDelegate) profilingDataProducer).getDelegate(),
               instanceOf(TestProfilingDataProducer.class));
  }

  @Test
  @Description("The correct discovery strategy is set")
  public void correctDiscoveryStrategy() {
    assertThat(profilingService.getDiscoveryStrategy(), instanceOf(CompositeProfilingDataConsumerDiscoveryStrategy.class));
    Set<ProfilingDataConsumer<? extends ProfilingEventContext>> profilingDataConsumers =
        profilingService.getDiscoveryStrategy().discover();
    assertThat(profilingDataConsumers, hasSize(6));
    assertThat(profilingDataConsumers, hasItem(is(instanceOf(LoggerByteBufferAllocationProfilingDataConsumer.class))));
    assertThat(profilingDataConsumers, hasItem(is(instanceOf(LoggerComponentThreadingDataConsumer.class))));
    assertThat(profilingDataConsumers, hasItem(is(instanceOf(LoggerByteBufferAllocationProfilingDataConsumer.class))));
    assertThat(profilingDataConsumers, hasItem(is(instanceOf(TaskSchedulingLoggerDataConsumer.class))));
    assertThat(profilingDataConsumers, hasItem(is(instanceOf(TransactionLoggerDataConsumer.class))));
    assertThat(profilingDataConsumers, hasItem(testProfilingDataConsumer));
  }

  @Test
  @Description("The notification listener is correctly set so that the notifications are managed")
  public void correctNotificationListenerSet() {
    verify(notificationManager, times(6)).addListenerSubscription(any(DefaultProfilingNotificationListener.class), any());
  }

  @Test
  @Description("When the data producer generates data, a notification is triggered")
  public void notificationTriggeredOnProfilingEvent() {
    ProfilingDataProducer<TestProfilingEventContext, Object> profilingDataProducer =
        profilingService.getProfilingDataProducer(TestProfilingEventType.TEST_PROFILING_EVENT_TYPE);
    profilingDataProducer
        .triggerProfilingEvent(new TestProfilingEventContext());

    verify(notificationManager).fireNotification(any(ProfilingNotification.class));
  }

  @Test
  @Description("When a generic component profiling event is produced a notification is triggered if it is enabled for a consumer")
  public void notificationTriggeredOnComponentProfilingEvent() throws Exception {
    ProfilingDataProducer<ExtensionProfilingEventContext, Object> profilingDataProducer =
        profilingService.getProfilingDataProducer(EXTENSION_PROFILING_EVENT);
    profilingDataProducer
        .triggerProfilingEvent(new TestComponentProfilingEventContext());
    verify(notificationManager).fireNotification(any(ProfilingNotification.class));

  }

  @Test
  @Description("When a operation started event is produced, then a notification is triggered")
  public void notificationTriggeredOnOperationStartedEvent() throws Exception {
    ProfilingDataProducer<ComponentThreadingProfilingEventContext, Object> profilingDataProducer =
        profilingService.getProfilingDataProducer(STARTING_OPERATION_EXECUTION);
    profilingDataProducer.triggerProfilingEvent(mock(ComponentThreadingProfilingEventContext.class));

    verify(notificationManager).fireNotification(any(ProfilingNotification.class));
  }

  /**
   * Stub for a {@link ProfilingDataConsumerDiscoveryStrategy}.
   */
  private class TestProfilingDataConsumerDiscoveryStrategy implements ProfilingDataConsumerDiscoveryStrategy {

    @Override
    public Set<ProfilingDataConsumer<?>> discover() {
      return ImmutableSet.of(testProfilingDataConsumer);
    }
  }

  /**
   * Stub for {@link ProfilingDataConsumer}.
   */
  private static class TestProfilingDataConsumer implements ProfilingDataConsumer<TestProfilingEventContext> {


    @Override
    public void onProfilingEvent(ProfilingEventType<TestProfilingEventContext> profilingEventType,
                                 TestProfilingEventContext profilingEventContext) {
      // Nothing to do.
    }

    @Override
    public Set<ProfilingEventType<TestProfilingEventContext>> getProfilingEventTypes() {
      return ImmutableSet.of(TestProfilingEventType.TEST_PROFILING_EVENT_TYPE);
    }

    @Override
    public Predicate<TestProfilingEventContext> getEventContextFilter() {
      return ctx -> true;
    }
  }

  /**
   * Stub for {@link ProfilingDataProducer}.
   */
  private static class TestProfilingDataProducer implements ProfilingDataProducer<TestProfilingEventContext, Object> {

    private final DefaultProfilingService defaultProfilingService;

    public TestProfilingDataProducer(DefaultProfilingService defaultProfilingService) {
      this.defaultProfilingService = defaultProfilingService;
    }

    @Override
    public void triggerProfilingEvent(TestProfilingEventContext profilingEventContext) {
      this.defaultProfilingService.notifyEvent(profilingEventContext, TestProfilingEventType.TEST_PROFILING_EVENT_TYPE);
    }

    @Override
    public void triggerProfilingEvent(Object baseInfo, Function<Object, TestProfilingEventContext> transformation) {
      this.defaultProfilingService.notifyEvent(transformation.apply(baseInfo), TestProfilingEventType.TEST_PROFILING_EVENT_TYPE);
    }
  }

  /**
   * Stub for testing profiling service.
   */
  private static class TestProfilingEventContext implements ProfilingEventContext {

    @Override
    public long getTriggerTimestamp() {
      return 0;
    }
  }

  /**
   * Stub for testing profiling service.
   */
  private static class TestComponentProfilingEventContext implements ExtensionProfilingEventContext {

    public static final String TEST_COMPONENT_ID = "TEST_COMPONENT_ID";
    public static final String TEST_COMPONENT_EVENT_ID = "TEST_COMPONENT_EVENT_ID";
    public static final String NON_EXISTENT = "NON_EXISTENT";

    @Override
    public long getTriggerTimestamp() {
      return 0;
    }

    @Override
    public String getProfilingDataSourceIdentifier() {
      return TEST_COMPONENT_ID;
    }

    @Override
    public String getExtensionEventSubtypeIdentifier() {
      return TEST_COMPONENT_EVENT_ID;
    }

    @Override
    public Optional<Object> get(String key) {
      return of(NON_EXISTENT);
    }
  }

  /**
   * Test {@link ProfilingEventType}
   */
  private enum TestProfilingEventType implements ProfilingEventType<TestProfilingEventContext> {

    TEST_PROFILING_EVENT_TYPE {

      public static final String TEST_PROFILING_NAMESPACE = "test-namespace";
      public static final String TEST_PROFILING_EVENT_IDENTIFIER = "test";

      @Override
      public String getProfilingEventTypeIdentifier() {
        return TEST_PROFILING_EVENT_IDENTIFIER;
      }

      @Override
      public String getProfilingEventTypeNamespace() {
        return TEST_PROFILING_NAMESPACE;
      }
    }
  }
}
