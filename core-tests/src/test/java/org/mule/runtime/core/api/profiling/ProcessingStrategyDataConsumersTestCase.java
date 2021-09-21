/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.profiling;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROFILING_SERVICE_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.profiling.consumer.LoggerComponentExecutionDataConsumer.ARTIFACT_ID_KEY;
import static org.mule.runtime.core.internal.profiling.consumer.LoggerComponentExecutionDataConsumer.ARTIFACT_TYPE_KEY;
import static org.mule.runtime.core.internal.profiling.consumer.LoggerComponentExecutionDataConsumer.LOCATION;
import static org.mule.runtime.core.internal.profiling.consumer.LoggerComponentExecutionDataConsumer.PROCESSING_THREAD_KEY;
import static org.mule.runtime.core.internal.profiling.consumer.LoggerComponentExecutionDataConsumer.PROFILING_EVENT_TIMESTAMP_KEY;
import static org.mule.runtime.core.internal.profiling.consumer.LoggerComponentExecutionDataConsumer.PROFILING_EVENT_TYPE;
import static org.mule.runtime.core.internal.profiling.consumer.LoggerComponentExecutionDataConsumer.RUNTIME_CORE_EVENT_CORRELATION_ID;
import static org.mule.runtime.core.internal.profiling.notification.ProfilingNotification.getFullyQualifiedProfilingNotificationIdentifier;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentExecutionProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.consumer.LoggerComponentExecutionDataConsumer;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentExecutionProfilingEventContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Feature(PROFILING)
@Story(DEFAULT_PROFILING_SERVICE)
@RunWith(Parameterized.class)
public class ProcessingStrategyDataConsumersTestCase extends AbstractMuleContextTestCase {

  public static final String THREAD_NAME = "threadName";
  public static final String ARTIFACT_ID = "artifactId";
  public static final String ARTIFACT_TYPE = "artifactType";
  public static final long PROFILING_EVENT_TIMESTAMP = 5678L;

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Rule
  public SystemProperty systemProperty = new SystemProperty(ENABLE_PROFILING_SERVICE_PROPERTY, "true");

  @Mock
  private CoreEvent event;

  @Mock
  private ComponentLocation location;

  @Mock
  private Logger logger;

  private final Gson gson = new Gson();

  private final ProfilingEventType<ComponentExecutionProfilingEventContext> profilingEventType;

  private ProfilingService profilingService;

  @Before
  public void before() throws Exception {
    when(logger.isDebugEnabled()).thenReturn(true);
    profilingService = getTestProfilingService();
  }

  private ProfilingService getTestProfilingService() throws MuleException {
    ProfilingService profilingService = new TestDefaultProfilingService(logger);
    initialiseIfNeeded(profilingService, muleContext);
    startIfNeeded(profilingService);
    return profilingService;
  }

  @Parameters(name = "eventType: {0}")
  public static Collection<ProfilingEventType<ComponentExecutionProfilingEventContext>> eventType() {
    return asList(PS_SCHEDULING_OPERATION_EXECUTION, STARTING_OPERATION_EXECUTION, OPERATION_EXECUTED,
                  PS_FLOW_MESSAGE_PASSING, PS_SCHEDULING_FLOW_EXECUTION, STARTING_FLOW_EXECUTION,
                  FLOW_EXECUTED);
  }

  public ProcessingStrategyDataConsumersTestCase(ProfilingEventType<ComponentExecutionProfilingEventContext> profilingEventType) {
    this.profilingEventType = profilingEventType;
  }

  @Test
  @Description("When a profiling event related to processing strategy is triggered, the data consumers process the data accordingly.")
  public void dataConsumersForProcessingStrategiesProfilingEventTypesConsumeDataAccordingly() {
    ProfilingDataProducer<ComponentExecutionProfilingEventContext> dataProducer =
        profilingService.getProfilingDataProducer(profilingEventType);

    DefaultComponentExecutionProfilingEventContext profilerEventContext =
        new DefaultComponentExecutionProfilingEventContext(event, location, THREAD_NAME, ARTIFACT_ID, ARTIFACT_TYPE,
                                                           PROFILING_EVENT_TIMESTAMP);
    dataProducer.triggerProfilingEvent(
                                       profilerEventContext);

    verify(logger).debug(jsonToLog(profilingEventType, profilerEventContext));
  }

  /**
   * Stub {@link DefaultProfilingService} with a test {@link ProfilingDataConsumerDiscoveryStrategy}.
   */
  private static class TestDefaultProfilingService extends DefaultProfilingService {

    private final Logger logger;

    public TestDefaultProfilingService(Logger logger) {
      this.logger = logger;

    }

    @Override
    public ProfilingDataConsumerDiscoveryStrategy getDiscoveryStrategy() {
      return new TestProfilingDataConsumerDiscoveryStrategy(logger);
    }

  }

  /**
   * Stub {@link ProfilingDataConsumerDiscoveryStrategy}
   */
  private static class TestProfilingDataConsumerDiscoveryStrategy implements ProfilingDataConsumerDiscoveryStrategy {

    private final Logger logger;

    public TestProfilingDataConsumerDiscoveryStrategy(Logger logger) {
      this.logger = logger;
    }

    @Override
    public Set<ProfilingDataConsumer<?>> discover() {
      return of(new TestLoggerComponentExecutionDataConsumer(logger));
    }
  }

  /**
   * Stub {@link LoggerComponentExecutionDataConsumer} for injecting a mocked {@link Logger}
   */
  private static class TestLoggerComponentExecutionDataConsumer extends LoggerComponentExecutionDataConsumer {

    private final Logger logger;

    public TestLoggerComponentExecutionDataConsumer(Logger logger) {
      super();
      this.logger = logger;
    }

    @Override
    protected Logger getDataConsumerLogger() {
      return logger;
    }
  }

  private String jsonToLog(ProfilingEventType<ComponentExecutionProfilingEventContext> profilingEventType,
                           ComponentExecutionProfilingEventContext profilingEventContext) {
    Map<String, String> eventMap = new HashMap<>();
    eventMap.put(PROFILING_EVENT_TYPE,
                 getFullyQualifiedProfilingNotificationIdentifier(profilingEventType));
    eventMap.put(PROFILING_EVENT_TIMESTAMP_KEY, Long.toString(profilingEventContext.getTriggerTimestamp()));
    eventMap.put(PROCESSING_THREAD_KEY, profilingEventContext.getThreadName());
    eventMap.put(ARTIFACT_ID_KEY, profilingEventContext.getArtifactId());
    eventMap.put(ARTIFACT_TYPE_KEY, profilingEventContext.getArtifactType());
    eventMap.put(RUNTIME_CORE_EVENT_CORRELATION_ID, profilingEventContext.getCorrelationId());
    profilingEventContext.getLocation().map(loc -> eventMap.put(LOCATION, loc.getLocation()));

    return gson.toJson(eventMap);
  }

}
