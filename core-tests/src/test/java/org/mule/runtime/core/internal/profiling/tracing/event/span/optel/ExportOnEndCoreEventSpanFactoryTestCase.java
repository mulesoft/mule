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
import static org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizer.getDefaultChildCustomizer;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.Thread.currentThread;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizer;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.ExportOnEndCoreEventSpanFactory;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizer;

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
  public static final String COMPONENT_LOCATION = "location";
  public static final String APP_ID = "appId";
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
  public void testOpentelemetryTracedSpanFactory() {
    CoreEvent coreEvent = mock(CoreEvent.class);
    EventContext coreEventContext = mock(EventContext.class);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(coreEvent.getContext()).thenReturn(coreEventContext);
    ComponentIdentifier componentIdentifier = mock(ComponentIdentifier.class);
    when(componentIdentifier.getNamespace()).thenReturn(IDENTIFIER_NAMESPACE);
    when(componentIdentifier.getName()).thenReturn(IDENTIFIER_NAME);

    InternalSpan span =
        coreEventSpanFactory.getSpan(coreEvent, muleConfiguration, APP, new SpanCustomizer() {

          @Override
          public String getName(CoreEvent coreEvent) {
            return getSpanName(componentIdentifier);
          }

          @Override
          public ChildSpanCustomizer getChildSpanCustomizer() {
            return getDefaultChildCustomizer();
          }

          @Override
          public Map<String, String> getAttributes(CoreEvent coreEvent, MuleConfiguration muleConfiguration,
                                                   ArtifactType artifactType) {
            return ImmutableMap.of(CORRELATION_ID_KEY, CORRELATION_ID, ARTIFACT_TYPE_KEY, APP.getAsString(), ARTIFACT_ID_KEY,
                                   APP_ID, THREAD_START_ID_KEY, String.valueOf(currentThread().getId()));
          }
        });
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
}
