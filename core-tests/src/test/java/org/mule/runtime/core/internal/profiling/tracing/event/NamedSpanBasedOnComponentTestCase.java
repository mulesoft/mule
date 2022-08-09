/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.util.Optional.of;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.NamedSpanBasedOnParentSpanChildCustomizerSpanCustomizer;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanInfo;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

import java.util.Map;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class NamedSpanBasedOnComponentTestCase {

  public static final String SPAN_NAME_SEPARATOR_FOR_TEST = ":";
  public static final String COMPONENT_IDENTIFIER_NAME = "component";
  public static final String COMPONENT_IDENTIFIER_NAMESPACE = "test";
  public static final String ROUTE = "route";
  public static final String PARENT_SPAN_NAMESPACE = "mule";
  public static final String PARENT_SPAN_NAME = "parent";
  public static final String CHILD_SPAN_NAME = "child";
  public static final String MULE_CONFIGURATION_ID = "id";
  public static final String LOCATION_TEST = "testLocation";
  public static final String LOCATION_KEY = "location";
  public static final String CORRELATION_ID = "correlationId";
  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String THREAD_START_ID = "threadStartId";
  public static final String THREAD_START_NAME = "threadStartName";
  public static final String ARTIFACT_TYPE = "artifactType";
  public static final String ARTIFACT_ID = "artifactId";

  @Test
  public void namedSpanBasedOnComponentIdentifierAloneSpanCustomizer() {
    Component component = mock(Component.class);
    ComponentIdentifier identifier = mock(ComponentIdentifier.class);
    ComponentLocation location = mock(ComponentLocation.class);

    when(component.getIdentifier()).thenReturn(identifier);
    when(identifier.getName()).thenReturn(COMPONENT_IDENTIFIER_NAME);
    when(identifier.getNamespace()).thenReturn(COMPONENT_IDENTIFIER_NAMESPACE);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.getId()).thenReturn(MULE_CONFIGURATION_ID);
    when(component.getLocation()).thenReturn(location);
    when(location.getLocation()).thenReturn(LOCATION_TEST);

    CoreEvent coreEvent = mock(CoreEvent.class);
    when(coreEvent.getCorrelationId()).thenReturn(CORRELATION_ID);

    SpanCustomizer spanCustomizer = new NamedSpanBasedOnComponentIdentifierAloneSpanCustomizer(component);

    assertThat(spanCustomizer.getName(coreEvent), equalTo(COMPONENT_IDENTIFIER_NAMESPACE + SPAN_NAME_SEPARATOR_FOR_TEST
        + COMPONENT_IDENTIFIER_NAME));
    assertThat(spanCustomizer.getChildSpanCustomizer().getChildSpanSuggestedName(),
               equalTo(SPAN_NAME_SEPARATOR_FOR_TEST + ROUTE));

    assertAttributes(spanCustomizer.getAttributes(coreEvent, muleConfiguration, APP));
  }

  @Test
  public void namedSpanBasedOnParentSpanChildCustomizerSpanCustomizer() {
    CoreEvent coreEvent = mock(CoreEvent.class);
    DistributedTraceContextAware eventContext =
        mock(DistributedTraceContextAware.class, withSettings().extraInterfaces(EventContext.class));
    DistributedTraceContext distributedTraceContext = mock(DistributedTraceContext.class);
    InternalSpan span = mock(InternalSpan.class);
    ChildSpanInfo childSpanInfo = mock(ChildSpanInfo.class);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.getId()).thenReturn(MULE_CONFIGURATION_ID);

    when(coreEvent.getCorrelationId()).thenReturn(CORRELATION_ID);
    when(coreEvent.getContext()).thenReturn((EventContext) eventContext);
    when(eventContext.getDistributedTraceContext()).thenReturn(distributedTraceContext);
    when(distributedTraceContext.getCurrentSpan()).thenReturn(of(span));
    when(childSpanInfo.getChildSpanSuggestedName()).thenReturn(CHILD_SPAN_NAME);
    when(span.getName()).thenReturn(PARENT_SPAN_NAMESPACE + SPAN_NAME_SEPARATOR_FOR_TEST + PARENT_SPAN_NAME);
    when(span.getChildSpanInfo()).thenReturn(childSpanInfo);
    when(span.getAttribute(LOCATION_KEY)).thenReturn(of(LOCATION_TEST));
    when(childSpanInfo.getChildSpanSuggestedName()).thenReturn(SPAN_NAME_SEPARATOR_FOR_TEST + CHILD_SPAN_NAME);

    SpanCustomizer spanCustomizer = new NamedSpanBasedOnParentSpanChildCustomizerSpanCustomizer();

    assertThat(spanCustomizer.getName(coreEvent), equalTo(PARENT_SPAN_NAMESPACE + SPAN_NAME_SEPARATOR_FOR_TEST + PARENT_SPAN_NAME
        + SPAN_NAME_SEPARATOR_FOR_TEST
        + CHILD_SPAN_NAME));
    assertThat(spanCustomizer.getChildSpanCustomizer().getChildSpanSuggestedName(), emptyString());

    assertAttributes(spanCustomizer.getAttributes(coreEvent, muleConfiguration, APP));
  }

  private void assertAttributes(Map<String, String> attributes) {
    assertThat(attributes.keySet(), hasSize(6));
    assertThat(attributes, hasEntry(LOCATION_KEY, LOCATION_TEST));
    assertThat(attributes, hasKey(THREAD_START_ID));
    assertThat(attributes, hasKey(THREAD_START_NAME));
    assertThat(attributes, hasEntry(ARTIFACT_TYPE, APP.getAsString()));
    assertThat(attributes, hasEntry(ARTIFACT_ID, MULE_CONFIGURATION_ID));
    assertThat(attributes, hasEntry(CORRELATION_ID_KEY, CORRELATION_ID));
  }

}
