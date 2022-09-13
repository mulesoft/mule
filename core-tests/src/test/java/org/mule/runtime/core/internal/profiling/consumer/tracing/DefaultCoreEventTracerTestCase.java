/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer.tracing;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.internal.event.trace.DistributedTraceContextGetter.emptyTraceContextMapGetter;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanIdentifier.componentSpanIdentifierFrom;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition.NO_CONDITION;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.DefaultCoreEventTracer.getCoreEventTracerBuilder;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getSpanName;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.NotNullSpanTracingCondition.getExistingCurrentSpanTracingCondition;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.NullSpanTracingCondition.getNoMuleCurrentSpanSetTracingCondition;
import static org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo.getDefaultChildSpanInfo;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static io.opentelemetry.api.trace.TraceState.getDefault;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.event.trace.EventDistributedTraceContext;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingConditionNotMetException;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.SpanNameTracingCondition;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporterVisitor;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.context.Context;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.slf4j.Logger;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class DefaultCoreEventTracerTestCase extends AbstractMuleTestCase {

  public static final String CORRELATION_ID = "correlationId";
  public static final String TEST_COMPONENT_IDENTIFIER_NAME = "test";
  public static final String TEST_COMPONENT_NAMESPACE_NAME = "test";
  public static final String TEST_COMPONENT_LOCATION = "location";
  public static final String TEST_APP = "test_app";
  public static final String KEY_1 = "key1";
  public static final String VALUE_1 = "value1";
  public static final String NON_EXPECTED_SPAN_NAME = "nonExpectedSpanName";
  public static final String EXPECTED_SPAN_NAME = "expectedSpanName";

  @Rule
  public ExpectedException expectedException = none();

  public static final String TRACEPARENT_KEY = "traceparent";
  public static final String TRACE_ID_SPAN_VALUE = "traceIdSpan";
  public static final String SPAN_ID_SPAN_VALUE = "spanIdSpan";

  @Test
  public void testStartComponentExecution() {
    MuleConfiguration mockedMuleConfiguration = mock(MuleConfiguration.class);
    when(mockedMuleConfiguration.getId()).thenReturn(TEST_APP);
    CoreEvent coreEvent = mock(CoreEvent.class);
    EventContext eventContext = mock(EventContext.class);
    Component component = mock(Component.class);
    ComponentIdentifier componentIdentifier = mock(ComponentIdentifier.class);
    ComponentLocation componentLocation = mock(ComponentLocation.class);

    when(coreEvent.getCorrelationId()).thenReturn(CORRELATION_ID);
    when(coreEvent.getContext()).thenReturn(eventContext);
    when(eventContext.getCorrelationId()).thenReturn(CORRELATION_ID);
    when(component.getIdentifier()).thenReturn(componentIdentifier);
    when(componentIdentifier.getName()).thenReturn(TEST_COMPONENT_IDENTIFIER_NAME);
    when(componentIdentifier.getNamespace()).thenReturn(TEST_COMPONENT_NAMESPACE_NAME);
    when(component.getLocation()).thenReturn(componentLocation);
    when(componentLocation.getLocation()).thenReturn(TEST_COMPONENT_LOCATION);

    CoreEventTracer coreEventTracer =
        getTestCoreEventTracer(TestSpanExportManager.getTestSpanExportManagerInstance(),
                               mock(Logger.class),
                               mockedMuleConfiguration);

    InternalSpan span = coreEventTracer.startComponentSpan(coreEvent, new SpanCustomizationInfo() {

      @Override
      public String getName(CoreEvent coreEvent) {
        return getSpanName(componentIdentifier);
      }

      @Override
      public ChildSpanCustomizationInfo getChildSpanCustomizationInfo() {
        return getDefaultChildSpanInfo();
      }
    }).orElse(null);

    assertThat(span, is(notNullValue()));
    assertThat(span.getName(), equalTo(getSpanName(component.getIdentifier())));
    assertThat(span.getParent(), nullValue());
    assertThat(span.getIdentifier(), equalTo(
                                             componentSpanIdentifierFrom(mockedMuleConfiguration.getId(),
                                                                         coreEvent.getCorrelationId())));
    assertThat(span.getParent(), nullValue());
    assertThat(span.getDuration().getStart(), notNullValue());
    assertThat(span.getDuration().getEnd(), nullValue());
  }

  @Test
  public void endCurrentExecutionSpan() {
    MuleConfiguration mockedMuleConfiguration = mock(MuleConfiguration.class);
    when(mockedMuleConfiguration.getId()).thenReturn(TEST_APP);
    CoreEventTracer coreEventTracer =
        getTestCoreEventTracer(TestSpanExportManager.getTestSpanExportManagerInstance(),
                               mock(Logger.class),
                               mockedMuleConfiguration);
    DistributedTraceContext distributedTraceContext = mock(DistributedTraceContext.class);
    coreEventTracer.endCurrentSpan(new FakeCoreEvent(new FakeCoreEventContext(distributedTraceContext)));

    verify(distributedTraceContext).endCurrentContextSpan(NO_CONDITION);
  }

  @Test
  public void getDistributedTraceContextMap() {
    MuleConfiguration mockedMuleConfiguration = mock(MuleConfiguration.class);
    when(mockedMuleConfiguration.getId()).thenReturn(TEST_APP);
    CoreEventTracer coreEventTracer =
        getTestCoreEventTracer(TestSpanExportManager.getTestSpanExportManagerInstance(),
                               mock(Logger.class),
                               mockedMuleConfiguration);
    DistributedTraceContext distributedTraceContext = mock(DistributedTraceContext.class);
    when(distributedTraceContext.tracingFieldsAsMap())
        .thenReturn(ImmutableMap.of(KEY_1, VALUE_1, TRACEPARENT_KEY, "traceparentvalue"));
    ExportOnEndSpan currentSpan = mock(ExportOnEndSpan.class);
    when(distributedTraceContext.getCurrentSpan()).thenReturn(of(currentSpan));
    Context mockedContext = mock(Context.class);
    when(currentSpan.visit(any())).thenReturn(of(mockedContext));
    Span span = mock(Span.class);
    when(mockedContext.get(any())).thenReturn(span);
    SpanContext spanContext = mock(SpanContext.class);
    when(span.getSpanContext()).thenReturn(spanContext);
    when(spanContext.getTraceId()).thenReturn(TRACE_ID_SPAN_VALUE);
    when(spanContext.getSpanId()).thenReturn(SPAN_ID_SPAN_VALUE);
    when(spanContext.getTraceFlags()).thenReturn(TraceFlags.getDefault());
    when(spanContext.getTraceState()).thenReturn(getDefault());
    when(spanContext.isValid()).thenReturn(true);
    Map<String, String> distributedTraceContextMap =
        coreEventTracer.getDistributedTraceContextMap(new FakeCoreEvent(new FakeCoreEventContext(distributedTraceContext)));
    assertThat(distributedTraceContextMap, hasEntry(KEY_1, VALUE_1));
    assertThat(distributedTraceContextMap.get(TRACEPARENT_KEY), StringContains.containsString(TRACE_ID_SPAN_VALUE));
    assertThat(distributedTraceContextMap.get(TRACEPARENT_KEY), StringContains.containsString(SPAN_ID_SPAN_VALUE));
    assertThat(distributedTraceContextMap, aMapWithSize(2));
  }

  @Test
  public void testStartComponentExecutionIfThrowable() {
    doTestErrorPropagation(false, "Error when starting a component span", (coreEventTracer, coreEvent) -> coreEventTracer
        .startComponentSpan(coreEvent, new TestSpanCustomizationInfo()));
  }

  @Test
  public void testStartComponentExecutionIfCurrentSpanNameVerificationFails() {
    // The Start Component Span Must Fail with the following Error
    expectTracingConditionNotException("The current span has name: " + NON_EXPECTED_SPAN_NAME + ".  Expected a span with name: "
        + EXPECTED_SPAN_NAME);

    // Creating a Core Event Tracer that propagates Exceptions.
    CoreEventTracer coreEventTracer = getTestCoreEventTracer(mock(Logger.class), true);

    // When the Start Component Span is executed, it must raise an exception indicating the current
    // span has other name.
    coreEventTracer.startComponentSpan(getCoreEventForTracingConditionTesting(NON_EXPECTED_SPAN_NAME),
                                       new TestSpanCustomizationInfo(),
                                       new SpanNameTracingCondition(EXPECTED_SPAN_NAME));
  }

  @Test
  public void testStartComponentExecutionIfCurrentSpanNameVerificationOk() {
    // Creating a Core Event Tracer that propagates exceptions
    CoreEventTracer coreEventTracer = getTestCoreEventTracer(mock(Logger.class), true);

    // As the expected current span name is the same as the one indicated in the tracing condition, a span must
    // be created
    Optional<InternalSpan> span = coreEventTracer.startComponentSpan(getCoreEventForTracingConditionTesting(EXPECTED_SPAN_NAME),
                                                                     new TestSpanCustomizationInfo(),
                                                                     new SpanNameTracingCondition(EXPECTED_SPAN_NAME));

    assertThat(span.isPresent(), equalTo(TRUE));
  }

  @Test
  public void testStartExecutionIfCurrentSpanNotSetConditionFails() {
    // The Start Component Span Must Fail with the following Error
    expectTracingConditionNotException("The current span is null. Expected a span with name: " + EXPECTED_SPAN_NAME);

    // Creating a Core Event Tracer that propagates exceptions
    CoreEventTracer coreEventTracer = getTestCoreEventTracer(mock(Logger.class), true);

    coreEventTracer.startComponentSpan(getCoreEventForTracingConditionTesting(null), new TestSpanCustomizationInfo(),
                                       new SpanNameTracingCondition(
                                                                    EXPECTED_SPAN_NAME));
  }

  @Test
  public void testEndSpanExecutionIfCurrentSpanNameVerificationOk() {
    // Creating a Core Event Tracer that propagates exceptions
    CoreEventTracer coreEventTracer = getTestCoreEventTracer(mock(Logger.class), true);

    // Creating a mock core event.
    CoreEvent coreEvent = getCoreEventForTracingConditionTesting(EXPECTED_SPAN_NAME);

    // Verifying that before the endCurrentSpan there is a current span present.
    assertThat(((DistributedTraceContextAware) coreEvent.getContext()).getDistributedTraceContext().getCurrentSpan().isPresent(),
               equalTo(TRUE));

    // Ending the current span, indicating the expected current span must have the current name.
    coreEventTracer.endCurrentSpan(coreEvent, new SpanNameTracingCondition(EXPECTED_SPAN_NAME));

    // As the expected current span name is the same as the one indicated in the tracing condition, no exception occurs and
    // there is no current span (it was ended).
    assertThat(((DistributedTraceContextAware) coreEvent.getContext()).getDistributedTraceContext().getCurrentSpan().isPresent(),
               equalTo(FALSE));
  }

  @Test
  public void testEndSpanIfCurrentSpanNotSetConditionFails() {
    // The End Current Span operation must fail with the following exception.
    expectTracingConditionNotException("The current span is null. Expected a span with name: " + EXPECTED_SPAN_NAME);

    // Creating a Core Event Tracer that propagates exceptions.
    CoreEventTracer coreEventTracer = getTestCoreEventTracer(mock(Logger.class), true);

    // The End Current Span must fail because we expect a span with the following name and there is no current span.
    coreEventTracer.endCurrentSpan(getCoreEventForTracingConditionTesting(null),
                                   new SpanNameTracingCondition(EXPECTED_SPAN_NAME));
  }

  @Test
  public void testStartComponentExecutionIfCurrentSpanNotSetConditionOk() {
    // Creating a Core Event Tracer that propagates exceptions.
    CoreEventTracer coreEventTracer = getTestCoreEventTracer(mock(Logger.class), true);

    // Creating a mock core event.
    CoreEvent coreEvent = getCoreEventForTracingConditionTesting(null);

    // Verifying that there is no current span set.
    assertThat(((DistributedTraceContextAware) coreEvent.getContext()).getDistributedTraceContext().getCurrentSpan().isPresent(),
               equalTo(FALSE));

    // Starts a Span with the tracing condition that no current span should be set.
    coreEventTracer.startComponentSpan(coreEvent, new TestSpanCustomizationInfo(),
                                       getNoMuleCurrentSpanSetTracingCondition());

    // We verify that now there is a current span.
    assertThat(((DistributedTraceContextAware) coreEvent.getContext()).getDistributedTraceContext().getCurrentSpan().isPresent(),
               equalTo(TRUE));
  }

  @Test
  public void testStartComponentExecutionIfCurrentSpanNotSetConditionFail() {
    // We expect an exception indicating that there is a current when no span was expected.
    expectTracingConditionNotException("Current span with name: " + NON_EXPECTED_SPAN_NAME
        + " was found while no current span was expected.");

    // Creating a Core Event Tracer that propagates exceptions.
    CoreEventTracer coreEventTracer = getTestCoreEventTracer(mock(Logger.class), true);

    // Creating a mock core event.
    CoreEvent coreEvent = getCoreEventForTracingConditionTesting(NON_EXPECTED_SPAN_NAME);

    coreEventTracer.startComponentSpan(coreEvent, new TestSpanCustomizationInfo(), getNoMuleCurrentSpanSetTracingCondition());
  }

  @Test
  public void testStartComponentExecutionIfCurrentSpanSetConditionOk() {
    // Creating a Core Event Tracer that propagates exceptions.
    CoreEventTracer coreEventTracer = getTestCoreEventTracer(mock(Logger.class), true);

    // Creating a mock core event.
    CoreEvent coreEvent = getCoreEventForTracingConditionTesting(EXPECTED_SPAN_NAME);

    // Starting a component span.
    coreEventTracer.startComponentSpan(coreEvent, new TestSpanCustomizationInfo(), getExistingCurrentSpanTracingCondition());

    assertThat(((DistributedTraceContextAware) coreEvent.getContext()).getDistributedTraceContext().getCurrentSpan().isPresent(),
               equalTo(TRUE));
  }

  @Test
  public void testStartComponentExecutionIfCurrentSpanSetConditionFail() {
    // We expect this exception.
    expectTracingConditionNotException("No current span set");

    // Creating a core event tracer that propagates exceptions.
    CoreEventTracer coreEventTracer = getTestCoreEventTracer(mock(Logger.class), true);

    // Creating a mock core event.
    CoreEvent coreEvent = getCoreEventForTracingConditionTesting(null);

    coreEventTracer.startComponentSpan(coreEvent, new TestSpanCustomizationInfo(),
                                       getExistingCurrentSpanTracingCondition());
  }

  @Test(expected = TracingErrorPropagationException.class)
  public void testStartComponentExecutionIfThrowableWithTracingErrorPropagationEnabled() {
    doTestErrorPropagation(true, "Error when starting a component span", (coreEventTracer, coreEvent) -> coreEventTracer
        .startComponentSpan(coreEvent, new TestSpanCustomizationInfo()));
  }

  @Test
  public void testEndCurrentSpanIfThrowable() {
    doTestErrorPropagation(false, "Error on ending current span", CoreEventTracer::endCurrentSpan);
  }

  @Test(expected = TracingErrorPropagationException.class)
  public void testEndCurrentSpanIfThrowableWithTracingErrorPropagationEnabled() {
    doTestErrorPropagation(true, null, CoreEventTracer::endCurrentSpan);
  }

  @Test
  public void testRecordErrorAtCurrentSpanIfThrowable() {
    doTestErrorPropagation(false, "Error recording a span error at current span",
                           (coreEventTracer, coreEvent) -> coreEventTracer.recordErrorAtCurrentSpan(coreEvent, true));
  }

  @Test(expected = TracingErrorPropagationException.class)
  public void testRecordErrorAtCurrentSpanIfThrowableWithTracingErrorPropagationEnabled() {
    doTestErrorPropagation(true, null, (coreEventTracer, coreEvent) -> coreEventTracer.recordErrorAtCurrentSpan(coreEvent, true));
  }

  @Test
  public void testRecordSpecifiedErrorAtCurrentSpanIfThrowable() {
    doTestErrorPropagation(false, "Error recording a span error at current span", (coreEventTracer, coreEvent) -> coreEventTracer
        .recordErrorAtCurrentSpan(coreEvent, true));
  }

  @Test(expected = TracingErrorPropagationException.class)
  public void testRecordSpecifiedErrorAtCurrentSpanIfThrowableWithTracingErrorPropagationEnabled() {
    doTestErrorPropagation(true, null, (coreEventTracer, coreEvent) -> coreEventTracer
        .recordErrorAtCurrentSpan(coreEvent, true));
  }

  @Test
  public void testGetDistributedTraceContextMapIfThrowable() {
    doTestErrorPropagation(false, "Error on getting distributed trace context", CoreEventTracer::getDistributedTraceContextMap);
  }

  @Test(expected = TracingErrorPropagationException.class)
  public void testGetDistributedTraceContextMapIfThrowableWithTracingErrorPropagationEnabled() {
    doTestErrorPropagation(true, null, CoreEventTracer::getDistributedTraceContextMap);
  }

  @NotNull
  private CoreEventTracer getTestCoreEventTracer(Logger loggerMock, boolean enablePropagateTracingErrors) {
    return getTestCoreEventTracer(TestSpanExportManager.getTestSpanExportManagerInstance(),
                                  mock(MuleConfiguration.class),
                                  loggerMock,
                                  enablePropagateTracingErrors);
  }

  private void expectTracingConditionNotException(String NON_EXPECTED_SPAN_NAME) {
    expectedException.expect(TracingConditionNotMetException.class);
    expectedException.expectMessage(NON_EXPECTED_SPAN_NAME);
  }

  private CoreEventTracer getTestCoreEventTracer(InternalSpanExportManager<EventContext> mockedSpanExporterManager,
                                                 Logger logger,
                                                 MuleConfiguration mockedMuleConfiguration) {
    return getTestCoreEventTracer(mockedSpanExporterManager, mockedMuleConfiguration, logger, false);
  }

  private CoreEvent getCoreEventForTracingConditionTesting(String eventContextCurrentSpanName) {
    CoreEvent coreEvent = mock(CoreEvent.class);
    DistributedTraceContextAware eventContext =
        mock(DistributedTraceContextAware.class, withSettings().extraInterfaces(EventContext.class));
    when(coreEvent.getContext()).thenReturn((EventContext) eventContext);
    DistributedTraceContext distributedTraceContext = EventDistributedTraceContext.builder()
        .withPropagateTracingExceptions(true).withGetter(
                                                         emptyTraceContextMapGetter())
        .build();
    when(eventContext.getDistributedTraceContext()).thenReturn(distributedTraceContext);

    if (eventContextCurrentSpanName == null) {
      return coreEvent;
    }

    InternalSpan currentSpan = mock(InternalSpan.class);
    when(currentSpan.getParent()).thenReturn(null);
    when(currentSpan.getName()).thenReturn(eventContextCurrentSpanName);
    distributedTraceContext.setCurrentSpan(currentSpan, span -> {
    });

    return coreEvent;
  }

  private void doTestErrorPropagation(boolean enablePropagateTracingErrors,
                                      String expectedLoggedMesage,
                                      BiConsumer<CoreEventTracer, CoreEvent> consumerToExecute) {
    Logger logger = mock(Logger.class);
    when(logger.isWarnEnabled()).thenReturn(true);
    CoreEventTracer coreEventTracer =
        getTestCoreEventTracer(logger, enablePropagateTracingErrors);
    CoreEvent coreEvent = mock(CoreEvent.class);
    when(coreEvent.getContext()).thenThrow(new TracingErrorPropagationException());
    consumerToExecute.accept(coreEventTracer, coreEvent);

    if (!enablePropagateTracingErrors) {
      verify(logger).warn(eq(expectedLoggedMesage), any(TracingErrorPropagationException.class));
    }
  }

  @NotNull
  private CoreEventTracer getTestCoreEventTracer(InternalSpanExportManager<EventContext> mockedSpanExporterManager,
                                                 MuleConfiguration mockedMuleConfiguration,
                                                 Logger logger,
                                                 boolean enablePropagateTracingErrors) {
    return getCoreEventTracerBuilder()
        .withSpanExporterManager(mockedSpanExporterManager)
        .withMuleConfiguration(mockedMuleConfiguration)
        .withArtifactType(APP)
        .withLogger(logger)
        .withPropagateTracingExceptions(enablePropagateTracingErrors)
        .build();
  }

  /**
   * A {@link InternalSpanExportManager} for testing purposes.
   */
  private static class TestSpanExportManager implements InternalSpanExportManager<EventContext> {

    public static TestSpanExportManager getTestSpanExportManagerInstance() {
      return new TestSpanExportManager();
    }

    @Override
    public InternalSpanExporter getInternalSpanExporter(EventContext context, MuleConfiguration muleConfiguration,
                                                        InternalSpan internalSpan) {
      return new InternalSpanExporter() {

        @Override
        public void export(InternalSpan internalSpan) {
          // nothing to do.
        }

        @Override
        public <T> T visit(InternalSpanExporterVisitor<T> internalSpanExporterVisitor) {
          return null;
        }
      };
    }
  }

  /**
   * A Fake for {@link CoreEvent}
   */
  private static class FakeCoreEvent implements CoreEvent {

    private final EventContext fakeCoreEventContext;

    public FakeCoreEvent(FakeCoreEventContext fakeCoreEventContext) {
      this.fakeCoreEventContext = fakeCoreEventContext;
    }

    @Override
    public Map<String, TypedValue<?>> getVariables() {
      return emptyMap();
    }

    @Override
    public Map<String, TypedValue<?>> getParameters() {
      return emptyMap();
    }

    @Override
    public Message getMessage() {
      return mock(Message.class);
    }

    @Override
    public Optional<Authentication> getAuthentication() {
      return empty();
    }

    @Override
    public Optional<Error> getError() {
      return empty();
    }

    @Override
    public String getCorrelationId() {
      return CORRELATION_ID;
    }

    @Override
    public Optional<ItemSequenceInfo> getItemSequenceInfo() {
      return empty();
    }

    @Override
    public EventContext getContext() {
      return fakeCoreEventContext;
    }

    @Override
    public BindingContext asBindingContext() {
      return mock(BindingContext.class);
    }

    @Override
    public SecurityContext getSecurityContext() {
      return mock(SecurityContext.class);
    }

    @Override
    public Optional<GroupCorrelation> getGroupCorrelation() {
      return empty();
    }

    @Override
    public FlowCallStack getFlowCallStack() {
      return mock(FlowCallStack.class);
    }
  }

  /**
   * Fake for {@link EventContext} that implements {@link DistributedTraceContextAware}
   */
  private static class FakeCoreEventContext implements EventContext, DistributedTraceContextAware {

    public static final String ROOT_ID = "rootId";
    private DistributedTraceContext distributedTraceContext;

    public FakeCoreEventContext(DistributedTraceContext distributedTraceContext) {
      this.distributedTraceContext = distributedTraceContext;
    }

    @Override
    public String getId() {
      return null;
    }

    @Override
    public String getRootId() {
      return ROOT_ID;
    }

    @Override
    public String getCorrelationId() {
      return CORRELATION_ID;
    }

    @Override
    public Instant getReceivedTime() {
      return null;
    }

    @Override
    public ComponentLocation getOriginatingLocation() {
      return mock(ComponentLocation.class);
    }

    @Override
    public DistributedTraceContext getDistributedTraceContext() {
      return distributedTraceContext;
    }

    @Override
    public void setDistributedTraceContext(DistributedTraceContext distributedTraceContext) {
      this.distributedTraceContext = distributedTraceContext;
    }
  }

  /**
   * A {@link RuntimeException} to test propagation of tracing exceptions.
   */
  private static class TracingErrorPropagationException extends RuntimeException {

  }

  /**
   * A {@link SpanCustomizationInfo} used for testing purposes.
   */
  private static class TestSpanCustomizationInfo implements SpanCustomizationInfo {

    @Override
    public String getName(CoreEvent coreEvent) {
      return "test";
    }

    @Override
    public ChildSpanCustomizationInfo getChildSpanCustomizationInfo() {
      return getDefaultChildSpanInfo();
    }
  }
}
