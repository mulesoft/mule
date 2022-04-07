/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.profiling;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.END_SPAN;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.START_SPAN;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROFILING_SERVICE_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.profiling.consumer.ComponentProfilingUtils.LOCATION;
import static org.mule.runtime.core.internal.profiling.consumer.ComponentProfilingUtils.getProcessingStrategyComponentInfoMap;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

import static java.util.Arrays.asList;

import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.internal.profiling.NoOpProfilingService;
import org.mule.runtime.core.internal.profiling.consumer.ComponentProcessingStrategyDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.ComponentSpanIdentifier;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.FlowSpanIdentifier;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentProcessingStrategyProfilingEventContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
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
  public static final String CORRELATION_ID = "correlationId";

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Rule
  public SystemProperty enableProfilingServiceProperty = new SystemProperty(ENABLE_PROFILING_SERVICE_PROPERTY, "true");

  @Rule
  public EnableInternalRuntimeProfilers enableInternalRuntimeProfilers =
      new EnableInternalRuntimeProfilers(new TestComponentProcessingStrategyDataConsumer(new NoOpProfilingService(), null));

  @Mock
  private CoreEvent event;

  @Mock
  private ComponentLocation location;

  @Mock
  private Logger logger;

  private final TestSpanDataConsumer testSpanDataConsumer = new TestSpanDataConsumer();

  @Mock
  private TypedComponentIdentifier componentIdentifier;

  @Mock
  private ComponentIdentifier identifier;

  private final Gson gson = new Gson();

  private final ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType;

  private ProfilingService profilingService;

  @Before
  public void before() throws Exception {
    when(logger.isDebugEnabled()).thenReturn(true);
    when(location.getComponentIdentifier()).thenReturn(componentIdentifier);
    when(location.getLocation()).thenReturn(LOCATION);
    when(componentIdentifier.getIdentifier()).thenReturn(identifier);
    when(identifier.getName()).thenReturn("test");
    when(identifier.getNamespace()).thenReturn("test");
    when(componentIdentifier.getType()).thenReturn(getTestComponentIdentifier());
    when(event.getCorrelationId()).thenReturn(CORRELATION_ID);
    profilingService = getTestProfilingService();
  }

  private TypedComponentIdentifier.ComponentType getTestComponentIdentifier() {
    if (profilingEventType == STARTING_FLOW_EXECUTION || profilingEventType == FLOW_EXECUTED) {
      return FLOW;
    } else {
      return OPERATION;
    }
  }

  private ProfilingService getTestProfilingService() throws MuleException {
    ProfilingService profilingService = new TestDefaultProfilingService(logger, testSpanDataConsumer);
    initialiseIfNeeded(profilingService, muleContext);
    startIfNeeded(profilingService);
    return profilingService;
  }

  @Parameters(name = "eventType: {0}")
  public static Collection<ProfilingEventType<ComponentProcessingStrategyProfilingEventContext>> eventType() {
    return asList(PS_SCHEDULING_OPERATION_EXECUTION, PS_STARTING_OPERATION_EXECUTION, PS_OPERATION_EXECUTED,
                  PS_FLOW_MESSAGE_PASSING, PS_SCHEDULING_FLOW_EXECUTION, STARTING_FLOW_EXECUTION,
                  FLOW_EXECUTED);
  }

  public ProcessingStrategyDataConsumersTestCase(
                                                 ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType) {
    this.profilingEventType = profilingEventType;
  }

  @Test
  @Description("When a profiling event related to processing strategy is triggered, the data consumers process the data accordingly.")
  public void dataConsumersForProcessingStrategiesProfilingEventTypesConsumeDataAccordingly() {
    ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, Object> dataProducer =
        profilingService.getProfilingDataProducer(profilingEventType);

    ComponentProcessingStrategyProfilingEventContext profilerEventContext =
        new DefaultComponentProcessingStrategyProfilingEventContext(event, location, THREAD_NAME, ARTIFACT_ID, ARTIFACT_TYPE,
                                                                    PROFILING_EVENT_TIMESTAMP);
    dataProducer.triggerProfilingEvent(profilerEventContext);
    verify(logger).debug(jsonToLog(profilingEventType, profilerEventContext));
    verifySpans(profilingEventType);
  }

  private void verifySpans(ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType) {
    Span profilingDataConsumerSpan = testSpanDataConsumer.getProfilingDataConsumerSpan();
    if (profilingEventType == PS_SCHEDULING_OPERATION_EXECUTION || profilingEventType == PS_FLOW_MESSAGE_PASSING ||
        profilingEventType == STARTING_FLOW_EXECUTION || profilingEventType == FLOW_EXECUTED) {
      assertThat(profilingDataConsumerSpan, is(notNullValue()));
      SpanIdentifier identifier = profilingDataConsumerSpan.getIdentifier();
      assertThat(identifier, instanceOf(getTestSpanIdentifierClass()));
      assertThat(identifier.getId(), equalTo(ARTIFACT_ID + "/" + LOCATION + "/" + CORRELATION_ID));

    } else {
      assertThat(profilingDataConsumerSpan, is(nullValue()));
    }
  }

  private Class getTestSpanIdentifierClass() {
    if (profilingEventType == STARTING_FLOW_EXECUTION || profilingEventType == FLOW_EXECUTED) {
      return FlowSpanIdentifier.class;
    } else {
      return ComponentSpanIdentifier.class;
    }
  }

  /**
   * Stub {@link DefaultProfilingService} with a test {@link ProfilingDataConsumerDiscoveryStrategy}.
   */
  private static class TestDefaultProfilingService extends DefaultProfilingService {

    private final Logger logger;
    private final TestSpanDataConsumer testSpanDataConsumer;

    public TestDefaultProfilingService(Logger logger, TestSpanDataConsumer testSpanDataConsumer) {
      this.testSpanDataConsumer = testSpanDataConsumer;
      this.logger = logger;

    }

    @Override
    public ProfilingDataConsumerDiscoveryStrategy getDiscoveryStrategy() {
      return new TestProfilingDataConsumerDiscoveryStrategy(this, testSpanDataConsumer, logger);
    }

  }

  /**
   * Stub {@link ProfilingDataConsumerDiscoveryStrategy}
   */
  private static class TestProfilingDataConsumerDiscoveryStrategy implements ProfilingDataConsumerDiscoveryStrategy {

    private final Logger logger;
    private final TestDefaultProfilingService profilingService;
    private final TestSpanDataConsumer testSpanDataConsumer;

    public TestProfilingDataConsumerDiscoveryStrategy(TestDefaultProfilingService profilingService,
                                                      TestSpanDataConsumer testSpanDataConsumer, Logger logger) {
      this.testSpanDataConsumer = testSpanDataConsumer;
      this.profilingService = profilingService;
      this.logger = logger;
    }

    @Override
    public Set<ProfilingDataConsumer<?>> discover() {
      return of(new TestComponentProcessingStrategyDataConsumer(profilingService, logger), testSpanDataConsumer);
    }
  }


  /**
   * Stub {@link ComponentProcessingStrategyDataConsumer} for injecting a mocked {@link Logger}
   */
  @RuntimeInternalProfilingDataConsumer
  private static class TestComponentProcessingStrategyDataConsumer extends ComponentProcessingStrategyDataConsumer {

    public TestComponentProcessingStrategyDataConsumer(InternalProfilingService profilingService, Logger logger) {
      super(profilingService, logger);
    }
  }

  private String jsonToLog(ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType,
                           ComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    return gson.toJson(getProcessingStrategyComponentInfoMap(profilingEventType, profilingEventContext));
  }

  /**
   * Stub {@link ProfilingDataConsumer} that consumes spans start/end profiling events.
   */
  @RuntimeInternalProfilingDataConsumer
  private static class TestSpanDataConsumer implements ProfilingDataConsumer<SpanProfilingEventContext> {

    private Span profilingDataConsumerSpan;

    @Override
    public void onProfilingEvent(ProfilingEventType<SpanProfilingEventContext> profilingEventType,
                                 SpanProfilingEventContext profilingEventContext) {
      profilingDataConsumerSpan = profilingEventContext.getSpan();
    }

    @Override
    public Set<ProfilingEventType<SpanProfilingEventContext>> getProfilingEventTypes() {
      return ImmutableSet.of(START_SPAN, END_SPAN);
    }

    @Override
    public Predicate<SpanProfilingEventContext> getEventContextFilter() {
      return context -> true;
    }

    public Span getProfilingDataConsumerSpan() {
      return profilingDataConsumerSpan;
    }
  }

}
