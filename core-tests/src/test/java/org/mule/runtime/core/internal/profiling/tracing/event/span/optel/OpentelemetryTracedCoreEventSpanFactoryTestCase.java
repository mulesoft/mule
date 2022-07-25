/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.optel;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getDefaultSpanExporterManager;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getSpanName;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.ExportOnEndCoreEventSpanFactory;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Matchers;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class OpentelemetryTracedCoreEventSpanFactoryTestCase {

  public static final String IDENTIFIER_NAMESPACE = "namespace";
  public static final String IDENTIFIER_NAME = "name";
  public static final String COMPONENT_LOCATION = "location";
  public static final String APP_ID = "appId";
  private final ExportOnEndCoreEventSpanFactory coreEventSpanFactory =
      new ExportOnEndCoreEventSpanFactory(getDefaultSpanExporterManager());

  @Test
  public void testOpentelemetryTracedSpanFactory() {
    CoreEvent coreEvent = mock(CoreEvent.class);
    Component component = mock(Component.class);
    EventContext coreEventContext = mock(EventContext.class);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(coreEvent.getContext()).thenReturn(coreEventContext);
    ComponentIdentifier componentIdentifier = mock(ComponentIdentifier.class);
    when(componentIdentifier.getNamespace()).thenReturn(IDENTIFIER_NAMESPACE);
    when(componentIdentifier.getName()).thenReturn(IDENTIFIER_NAME);
    when(component.getIdentifier()).thenReturn(componentIdentifier);
    ComponentLocation componentLocation = mock(ComponentLocation.class);
    when(componentLocation.getLocation()).thenReturn(COMPONENT_LOCATION);
    when(muleConfiguration.getId()).thenReturn(APP_ID);
    when(component.getLocation()).thenReturn(componentLocation);

    InternalSpan span = coreEventSpanFactory.getSpan(coreEvent, component, muleConfiguration);
    assertThat(span, instanceOf(ExportOnEndSpan.class));

    ExportOnEndSpan opentelemetryExecutionSpan = (ExportOnEndSpan) span;
    assertThat(opentelemetryExecutionSpan.getName(), Matchers.equalTo(getSpanName(componentIdentifier)));
  }
}
