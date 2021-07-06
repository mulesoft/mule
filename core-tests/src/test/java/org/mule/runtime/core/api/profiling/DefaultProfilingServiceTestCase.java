/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.profiling;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_DISPATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_HANDLER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.profiling.ProfilerDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.DefaultProfilingNotificationListener;
import org.mule.runtime.core.internal.profiling.notification.ProfilingNotification;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

  @Mock
  private ServerNotificationManager notificationManager;

  @Mock
  protected FeatureFlaggingService featureFlaggingService;

  private DefaultProfilingService profilingService;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();
    super.getStartUpRegistryObjects().putAll(objects);
    objects.put(OBJECT_NOTIFICATION_DISPATCHER, notificationManager);
    objects.put(OBJECT_NOTIFICATION_HANDLER, notificationManager);
    return objects;
  }

  @Before
  public void configureProfilingService() throws MuleException {
    profilingService = new TestDefaultProfilingService();
    initialiseIfNeeded(profilingService, muleContext);
    when(featureFlaggingService.isEnabled(MuleRuntimeFeature.ENABLE_PROFILING_SERVICE)).thenReturn(true);
    profilingService.setFeatureFlags(featureFlaggingService);
    startIfNeeded(profilingService);
    profilingService.registerProfilingDataProducer(TestProfilingEventType.TEST_PROFILING_EVENT_TYPE,
                                                   new TestProfilingDataProducer(profilingService));
  }

  @Test
  @Description("When profiling data consumers are obtained, the correct data producers are returned")
  public void correctDataProducersObtained() {
    ProfilingDataProducer<TestProfilingEventContext> profilingDataProducer =
        profilingService.getProfilingDataProducer(TestProfilingEventType.TEST_PROFILING_EVENT_TYPE);
    assertThat(profilingDataProducer, instanceOf(TestProfilingDataProducer.class));
  }

  @Test
  @Description("The correct discovery strategy is set")
  public void correctDiscoveryStrategy() {
    assertThat(profilingService.getDiscoveryStrategy(), instanceOf(TestProfilerDataConsumerDiscoveryStrategy.class));
  }

  @Test
  @Description("The notification listener is correctly set so that the notifications are managed")
  public void correctNotificationListenerSet() {
    verify(notificationManager).addListenerSubscription(any(DefaultProfilingNotificationListener.class), any());
  }

  @Test
  @Description("When the data producer generates data, a notification is triggered")
  public void notificationTriggeredOnProfilingEvent() {
    ProfilingDataProducer<TestProfilingEventContext> profilingDataProducer =
        profilingService.getProfilingDataProducer(TestProfilingEventType.TEST_PROFILING_EVENT_TYPE);
    profilingDataProducer
        .trigggerProfilingEvent(new TestProfilingEventContext());

    verify(notificationManager).fireNotification(any(ProfilingNotification.class));
  }

  /**
   * Stub {@link DefaultProfilingService} with a test {@link ProfilerDataConsumerDiscoveryStrategy}.
   */
  private static class TestDefaultProfilingService extends DefaultProfilingService {

    @Override
    public ProfilerDataConsumerDiscoveryStrategy getDiscoveryStrategy() {
      return new TestProfilerDataConsumerDiscoveryStrategy();
    }
  }

  /**
   * Stub for a {@link ProfilerDataConsumerDiscoveryStrategy}.
   */
  private static class TestProfilerDataConsumerDiscoveryStrategy implements ProfilerDataConsumerDiscoveryStrategy {

    @Override
    public <S extends ProfilingDataConsumer<T>, T extends ProfilingEventContext> Set<S> discover() {
      return (Set<S>) ImmutableSet.of(new TestProfilingDataConsumer());
    }
  }

  /**
   * Stub for {@link ProfilingDataConsumer}.
   */
  private static class TestProfilingDataConsumer implements ProfilingDataConsumer<TestProfilingEventContext> {

    @Override
    public void onProfilingEvent(String profilerEventIdentifier, TestProfilingEventContext profilerEventContext) {
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
  private static class TestProfilingDataProducer implements ProfilingDataProducer<TestProfilingEventContext> {

    private final DefaultProfilingService defaultProfilingService;

    public TestProfilingDataProducer(DefaultProfilingService defaultProfilingService) {
      this.defaultProfilingService = defaultProfilingService;
    }

    @Override
    public void trigggerProfilingEvent(TestProfilingEventContext profilerEventContext) {
      this.defaultProfilingService.notifyEvent(profilerEventContext, TestProfilingEventType.TEST_PROFILING_EVENT_TYPE);
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
   * Test {@link ProfilingEventType}
   */
  private enum TestProfilingEventType implements ProfilingEventType<TestProfilingEventContext> {

    TEST_PROFILING_EVENT_TYPE {

      @Override
      public String getProfilingEventTypeIdentifier() {
        return "test";
      }

      @Override
      public String getProfilerEventTypeNamespace() {
        return "test-namespace";
      }

    }

  }
}
