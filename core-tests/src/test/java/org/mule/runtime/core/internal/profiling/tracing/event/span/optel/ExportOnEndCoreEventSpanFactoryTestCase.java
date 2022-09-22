/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.optel;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getDefaultSpanExporterManager;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getSpanName;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.NoExportNamedSpanBasedOnParentSpanChildSpanCustomizationInfo.getNoExportChildNamedSpanBasedOnParentSpanChildSpanCustomizationInfo;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Optional.of;
import static java.lang.Thread.currentThread;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.NoChildrenExportableNamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.internal.profiling.tracing.export.NoExportableOpenTelemetrySpan;
import org.mule.runtime.core.internal.profiling.tracing.export.OpenTelemetrySpanExporter;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.ExportOnEndCoreEventSpanFactory;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class ExportOnEndCoreEventSpanFactoryTestCase {

  public static final String IDENTIFIER_NAMESPACE = "namespace";
  public static final String IDENTIFIER_NAME = "name";
  public static final String APP_ID = "appId";
  public static final ChildSpanCustomizationInfo DEFAULT_CHILD_SPAN_INFO = ChildSpanCustomizationInfo.getDefaultChildSpanInfo();
  public static final String MULE_DUMMY_COMPONENT_NAME = "mule:dummy";
  private final ExportOnEndCoreEventSpanFactory coreEventSpanFactory =
      new ExportOnEndCoreEventSpanFactory(getDefaultSpanExporterManager());
  public static final String CORRELATION_ID = "000-000-0000";
  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String ARTIFACT_TYPE_KEY = "artifactType";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String THREAD_START_ID_KEY = "threadStartId";
  public static final String THREAD_END_ID = "threadEndId";
  public static final String THREAD_END_ID_KEY = THREAD_END_ID;

  @Test
  public void testOpenTelemetryTracedSpanFactory() {
    ComponentIdentifier componentIdentifier = mock(ComponentIdentifier.class);
    when(componentIdentifier.getNamespace()).thenReturn(IDENTIFIER_NAMESPACE);
    when(componentIdentifier.getName()).thenReturn(IDENTIFIER_NAME);

    InternalSpan spanMock = mock(InternalSpan.class);
    when(spanMock.getParent()).thenReturn(null);
    setSpanMock(spanMock);
    InternalSpan span = getSpan(new SpanCustomizationInfo() {

      @Override
      public String getName(CoreEvent coreEvent) {
        return getSpanName(componentIdentifier);
      }

      @Override
      public ChildSpanCustomizationInfo getChildSpanCustomizationInfo() {
        return DEFAULT_CHILD_SPAN_INFO;
      }

      @Override
      public Map<String, String> getAttributes(CoreEvent coreEvent, MuleConfiguration muleConfiguration,
                                               ArtifactType artifactType) {
        return ImmutableMap.of(CORRELATION_ID_KEY, CORRELATION_ID, ARTIFACT_TYPE_KEY, APP.getAsString(), ARTIFACT_ID_KEY,
                               APP_ID, THREAD_START_ID_KEY, String.valueOf(currentThread().getId()));
      }
    }, spanMock);
    assertThat(span, instanceOf(ExportOnEndSpan.class));

    ExportOnEndSpan exportOnEndSpan = (ExportOnEndSpan) span;
    assertThat(exportOnEndSpan.getName(), equalTo(getSpanName(componentIdentifier)));
    assertThat(exportOnEndSpan.getAttribute(CORRELATION_ID_KEY).orElse(null), equalTo(CORRELATION_ID));
    assertThat(exportOnEndSpan.getAttribute(ARTIFACT_TYPE_KEY).orElse(null), equalTo(APP.getAsString()));
    assertThat(exportOnEndSpan.getAttribute(ARTIFACT_ID_KEY).orElse(null), equalTo(APP_ID));
    assertThat(exportOnEndSpan.getAttribute(THREAD_START_ID_KEY).orElse(null), notNullValue());
    assertThat(exportOnEndSpan.getAttribute(THREAD_END_ID_KEY).orElse(null), nullValue());
    assertThat(span.getDuration().getStart(), notNullValue());
  }

  private void setSpanMock(InternalSpan spanMock) {
    when(spanMock.getName()).thenReturn(MULE_DUMMY_COMPONENT_NAME);
    ChildSpanCustomizationInfo childSpanInfo = mock(ChildSpanCustomizationInfo.class);
    when(spanMock.getChildSpanInfo()).thenReturn(childSpanInfo);
  }

  @Test
  public void whenNewExportLevelIsLessThanParentLevelReturnNewLevel() {
    doTestLevels(3, 1);
  }

  @Test
  public void whenNewExportLevelIsMoreThanParentLevelParentLevelMinus1() {
    doTestLevels(0, -1);
  }

  @Test
  public void testExportOnEndCoreOpenTelemetryExporterNotExportable() {
    Component component = mock(Component.class);
    ComponentIdentifier componentIdentifier = mock(ComponentIdentifier.class);
    when(componentIdentifier.getNamespace()).thenReturn(IDENTIFIER_NAMESPACE);
    when(componentIdentifier.getName()).thenReturn(IDENTIFIER_NAME);
    when(component.getIdentifier()).thenReturn(componentIdentifier);
    InternalSpan spanMock = mock(InternalSpan.class);
    when(spanMock.getParent()).thenReturn(null);
    setSpanMock(spanMock);
    InternalSpan span = getSpan(getNoExportChildNamedSpanBasedOnParentSpanChildSpanCustomizationInfo(), spanMock);

    assertThat(span, instanceOf(ExportOnEndSpan.class));
    InternalSpanExporter spanExporter = ((ExportOnEndSpan) span).getSpanExporter();
    assertThat(spanExporter, instanceOf(OpenTelemetrySpanExporter.class));
    OpenTelemetrySpanExporter openTelemetrySpanExporter = (OpenTelemetrySpanExporter) spanExporter;
    assertThat(openTelemetrySpanExporter.getExportUntilLevel(), equalTo(MAX_VALUE - 1));
    assertThat(openTelemetrySpanExporter.getOpenTelemetrySpan(), instanceOf(NoExportableOpenTelemetrySpan.class));
  }

  private InternalSpan getSpan(SpanCustomizationInfo spanCustomizationInfo, InternalSpan currentSpan) {
    CoreEvent coreEvent = mock(CoreEvent.class);
    EventContext coreEventContext = mock(EventContext.class, withSettings().extraInterfaces(DistributedTraceContextAware.class));
    DistributedTraceContext distributedTraceContext = mock(DistributedTraceContext.class);
    when(distributedTraceContext.getCurrentSpan()).thenReturn(of(currentSpan));
    when(((DistributedTraceContextAware) coreEventContext).getDistributedTraceContext()).thenReturn(distributedTraceContext);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(coreEvent.getContext()).thenReturn(coreEventContext);

    return coreEventSpanFactory.getSpan(coreEvent, muleConfiguration, APP, spanCustomizationInfo);
  }


  private void doTestLevels(int parentLevel, int expectedLevel) {
    Component component = mock(Component.class);
    ComponentIdentifier componentIdentifier = mock(ComponentIdentifier.class);
    when(componentIdentifier.getNamespace()).thenReturn(IDENTIFIER_NAMESPACE);
    when(componentIdentifier.getName()).thenReturn(IDENTIFIER_NAME);
    when(component.getIdentifier()).thenReturn(componentIdentifier);
    ExportOnEndSpan spanMock = mock(ExportOnEndSpan.class);
    setSpanMock(spanMock);
    when(spanMock.getParent()).thenReturn(null);
    InternalSpanExporter internalSpanExporter = mock(InternalSpanExporter.class);
    when(spanMock.getSpanExporter()).thenReturn(internalSpanExporter);
    when(internalSpanExporter.getExportUntilLevel()).thenReturn(parentLevel);
    InternalSpan span = getSpan(new NoChildrenExportableNamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo(component),
                                spanMock);

    assertThat(span, instanceOf(ExportOnEndSpan.class));
    InternalSpanExporter spanExporter = ((ExportOnEndSpan) span).getSpanExporter();
    assertThat(spanExporter, instanceOf(OpenTelemetrySpanExporter.class));
    OpenTelemetrySpanExporter openTelemetrySpanExporter = (OpenTelemetrySpanExporter) spanExporter;
    assertThat(openTelemetrySpanExporter.getExportUntilLevel(), equalTo(expectedLevel));
    if (expectedLevel <= 0) {
      assertThat(openTelemetrySpanExporter.getOpenTelemetrySpan(), instanceOf(NoExportableOpenTelemetrySpan.class));
    } else {
      assertThat(openTelemetrySpanExporter.getOpenTelemetrySpan(), not(instanceOf(NoExportableOpenTelemetrySpan.class)));
    }
  }
}
