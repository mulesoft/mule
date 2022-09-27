/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.parameter;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.DefaultCoreEventTracer.getCoreEventTracerBuilder;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import static java.util.Optional.empty;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpanVisitor;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import java.util.Map;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.slf4j.Logger;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class PropagateAllDistributedTraceContextManagerTestCase {


  public static final String TRACE_FIELD_1 = "traceField1";
  public static final String TRACE_FIELD_VALUE_1 = "traceFieldValue1";
  public static final String TRACE_FIELD_2 = "traceField2";
  public static final String TRACE_FIELD_VALUE_2 = "traceFieldValue2";
  public static final String TRACE_FIELD_3 = "traceField3";
  public static final String TRACE_FIELD_VALUE_3 = "traceFieldValue3";
  public static final String TRACE_FIELD_4 = "traceField4";
  public static final String TRACE_FIELD_VALUE_4 = "traceFieldValue4";

  @Test
  public void propagatesAllFieldsInBaggageAndTraceFields() {
    DistributedTraceContext distributedTraceContext = mock(DistributedTraceContext.class);
    when(distributedTraceContext.tracingFieldsAsMap())
        .thenReturn(of(TRACE_FIELD_1, TRACE_FIELD_VALUE_1, TRACE_FIELD_2, TRACE_FIELD_VALUE_2));
    when(distributedTraceContext.baggageItemsAsMap())
        .thenReturn(of(TRACE_FIELD_3, TRACE_FIELD_VALUE_3, TRACE_FIELD_4, TRACE_FIELD_VALUE_4));
    ExportOnEndSpan span = mock(ExportOnEndSpan.class);
    when(distributedTraceContext.getCurrentSpan()).thenReturn(Optional.of(span));
    when(span.visit(any(InternalSpanVisitor.class))).thenReturn(empty());
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    InternalSpanExportManager<EventContext> spanExportManager = mock(InternalSpanExportManager.class);
    Logger logger = mock(Logger.class);
    CoreEvent coreEvent = mock(CoreEvent.class);
    EventContext eventContext = mock(EventContext.class, withSettings().extraInterfaces(DistributedTraceContextAware.class));
    when(coreEvent.getContext()).thenReturn(eventContext);
    when(((DistributedTraceContextAware) eventContext).getDistributedTraceContext()).thenReturn(distributedTraceContext);
    PropagateAllDistributedTraceContextManager manager = new PropagateAllDistributedTraceContextManager(coreEvent,
                                                                                                        getCoreEventTracerBuilder()
                                                                                                            .withMuleConfiguration(muleConfiguration)
                                                                                                            .withArtifactType(APP)
                                                                                                            .withLogger(logger)
                                                                                                            .withSpanExporterManager(spanExportManager)
                                                                                                            .build());

    Map<String, String> remoteContextMap = manager.getRemoteTraceContextMap();

    assertThat(remoteContextMap, hasEntry(TRACE_FIELD_1, TRACE_FIELD_VALUE_1));
    assertThat(remoteContextMap, hasEntry(TRACE_FIELD_2, TRACE_FIELD_VALUE_2));
    assertThat(remoteContextMap, hasEntry(TRACE_FIELD_3, TRACE_FIELD_VALUE_3));
    assertThat(remoteContextMap, hasEntry(TRACE_FIELD_4, TRACE_FIELD_VALUE_4));
  }

}
