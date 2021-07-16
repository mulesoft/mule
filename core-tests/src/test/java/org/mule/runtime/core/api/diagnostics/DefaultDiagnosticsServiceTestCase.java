/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

import static java.lang.System.currentTimeMillis;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_DISPATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_HANDLER;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.OPERATION_EXECUTED;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_DISPATCH;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_END;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.test.allure.AllureConstants.Diagnostics.DIAGNOSTICS;
import static org.mule.test.allure.AllureConstants.Diagnostics.DiagnosticsServiceStory.DEFAULT_DIAGNOSTICS_SERVICE;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.diagnostics.consumer.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.diagnostics.consumer.context.ProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.diagnostics.notification.DefaultProfilingNotificationListener;
import org.mule.runtime.core.api.diagnostics.notification.ProfilingNotification;
import org.mule.runtime.core.api.diagnostics.producer.ComponentProcessingStrategyProfilingDataProducer;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Feature(DIAGNOSTICS)
@Story(DEFAULT_DIAGNOSTICS_SERVICE)
public class DefaultDiagnosticsServiceTestCase extends AbstractMuleContextTestCase {

  public static final String THREAD_NAME = "threadName";

  public static final String ARTIFACT_ID = "artifactId";

  public static final String ARTIFACT_TYPE = "artifactType";

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private ServerNotificationManager notificationManager;

  @Mock
  protected FeatureFlaggingService featureFlaggingService;

  @Mock
  private CoreEvent coreEvent;

  @Mock
  private ComponentLocation location;

  private DefaultDiagnosticsService diagnosticsService;


  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();
    super.getStartUpRegistryObjects().putAll(objects);
    objects.put(OBJECT_NOTIFICATION_DISPATCHER, notificationManager);
    objects.put(OBJECT_NOTIFICATION_HANDLER, notificationManager);
    return objects;
  }

  @Before
  public void configureDiagnosticService() throws MuleException {
    diagnosticsService = new DefaultDiagnosticsService();
    initialiseIfNeeded(diagnosticsService, muleContext);
    when(featureFlaggingService.isEnabled(MuleRuntimeFeature.ENABLE_DIAGNOSTICS_SERVICE)).thenReturn(true);
    diagnosticsService.setFeatureFlags(featureFlaggingService);
    startIfNeeded(diagnosticsService);
  }

  @Test
  @Description("When profiling data consumers are obtained, the correct data producers are returned")
  public void correctDataProducersObtained() {
    assertCorrectDataProducerFor(OPERATION_EXECUTED);
    assertCorrectDataProducerFor(PS_FLOW_END);
    assertCorrectDataProducerFor(PS_FLOW_DISPATCH);
    assertCorrectDataProducerFor(PS_FLOW_MESSAGE_PASSING);
    assertCorrectDataProducerFor(PS_SCHEDULING_OPERATION_EXECUTION);
    assertCorrectDataProducerFor(OPERATION_EXECUTED);
  }

  @Test
  @Description("The correct discovery strategy is set")
  public void correctDiscoveryStrategy() {
    assertThat(diagnosticsService.getDiscoveryStrategy(), instanceOf(DefaultProfilerDataConsumerDiscoveryStrategy.class));
  }

  @Test
  @Description("The notification listener is correctly set so that the notifications are managed")
  public void correctNotificationListenerSet() {
    verify(notificationManager).addListenerSubscription(any(DefaultProfilingNotificationListener.class), any());
  }

  @Test
  @Description("When the data producer generates data, a notification is triggered")
  public void notificationTriggeredOnProfilingEvent() {
    ProfilingDataProducer<ProcessingStrategyProfilingEventContext> profilingDataProducer =
        diagnosticsService.getProfilingDataProducer(OPERATION_EXECUTED);
    profilingDataProducer
        .event(new ComponentProcessingStrategyProfilingEventContext(coreEvent, location, THREAD_NAME, ARTIFACT_ID,
                                                                    ARTIFACT_TYPE,
                                                                    currentTimeMillis()));

    verify(notificationManager).fireNotification(any(ProfilingNotification.class));

  }


  private void assertCorrectDataProducerFor(ProfilingEventType<ProcessingStrategyProfilingEventContext> profilingEventType) {
    ProfilingDataProducer<ProcessingStrategyProfilingEventContext> profilingDataProducer =
        diagnosticsService.getProfilingDataProducer(profilingEventType);
    assertThat(profilingDataProducer, instanceOf(ComponentProcessingStrategyProfilingDataProducer.class));
  }

}
