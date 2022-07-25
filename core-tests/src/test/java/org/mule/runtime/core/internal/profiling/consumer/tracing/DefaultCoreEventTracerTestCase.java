/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer.tracing;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanIdentifier.componentSpanIdentifierFrom;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.DefaultCoreEventTracer.getCoreEventTracerBuilder;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getSpanName;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporterVisitor;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class DefaultCoreEventTracerTestCase {

  public static final String CORRELATION_ID = "correlationId";
  public static final String TEST_COMPONENT_IDENTIFIER_NAME = "test";
  public static final String TEST_COMPONENT_NAMESPACE_NAME = "test";
  public static final String TEST_COMPONENT_LOCATION = "location";
  public static final String TEST_APP = "test_app";

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
                               mockedMuleConfiguration);

    InternalSpan span = coreEventTracer.startComponentSpan(coreEvent, component);

    assertThat(span.getName(), equalTo(getSpanName(component.getIdentifier())));
    assertThat(span.getParent(), nullValue());
    assertThat(span.getIdentifier(), equalTo(
                                             componentSpanIdentifierFrom(mockedMuleConfiguration.getId(), component.getLocation(),
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
                               mockedMuleConfiguration);
    DistributedTraceContext distributedTraceContext = mock(DistributedTraceContext.class);
    coreEventTracer.endCurrentSpan(new FakeCoreEvent(new FakeCoreEventContext(distributedTraceContext)));

    verify(distributedTraceContext).endCurrentContextSpan();
  }

  @NotNull
  private CoreEventTracer getTestCoreEventTracer(InternalSpanExportManager<EventContext> mockedSpanExporterManager,
                                                 MuleConfiguration mockedMuleConfiguration) {
    return getCoreEventTracerBuilder()
        .withSpanExporterManager(mockedSpanExporterManager)
        .withMuleConfiguration(mockedMuleConfiguration)
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
    public InternalSpanExporter getInternalSpanExporter(EventContext context, InternalSpan internalSpan) {
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
}
