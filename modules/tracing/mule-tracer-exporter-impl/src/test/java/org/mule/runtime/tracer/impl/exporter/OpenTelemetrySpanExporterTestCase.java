/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.System.nanoTime;
import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.impl.exporter.optel.span.NoopMuleOpenTelemetrySpan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunWith(Parameterized.class)
public class OpenTelemetrySpanExporterTestCase {

  public static final String ARTIFACT_ID = "artifactId";
  public static final String ARTIFACT_TYPE = "artifactType";
  public static final String SPAN_NAME = "spanName";
  private final Class expectedExportableClass;
  private final boolean exportable;
  private final boolean isRoot;
  private final boolean isPolicy;

  @Parameterized.Parameters(name = "exportable: {0}, expectedExportableClass: {1}, isPolicy: {2}, isRoot: {3}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {true, DecoratedMuleOpenTelemetrySpan.class, true, false},
        {true, DecoratedMuleOpenTelemetrySpan.class, false, false},
        {true, DecoratedMuleOpenTelemetrySpan.class, false, true},
        {true, DecoratedMuleOpenTelemetrySpan.class, true, true},
        {false, NoopMuleOpenTelemetrySpan.class, true, false},
        {false, NoopMuleOpenTelemetrySpan.class, false, false},
        {false, NoopMuleOpenTelemetrySpan.class, false, true},
        {false, NoopMuleOpenTelemetrySpan.class, true, true},
    });
  }

  public OpenTelemetrySpanExporterTestCase(boolean exportable, Class expectedExportableClass, boolean isPolicy, boolean isRoot) {
    this.expectedExportableClass = expectedExportableClass;
    this.exportable = exportable;
    this.isRoot = isRoot;
    this.isPolicy = isPolicy;
  }

  @Test
  public void generateExportableOpenTelemetrySpan() {
    InternalSpan internalSpan = mock(InternalSpan.class);
    when(internalSpan.getName()).thenReturn(SPAN_NAME);
    SpanDuration spanDuration = mock(SpanDuration.class);
    when(spanDuration.getStart()).thenReturn(nanoTime());
    when(internalSpan.getDuration()).thenReturn(spanDuration);

    InitialSpanInfo initialSpanInfo = mock(InitialSpanInfo.class);
    when(initialSpanInfo.isPolicySpan()).thenReturn(isPolicy);
    when(initialSpanInfo.isRootSpan()).thenReturn(isRoot);
    when(initialSpanInfo.getInitialExportInfo()).thenReturn(new InitialExportInfo() {

      @Override
      public boolean isExportable() {
        return exportable;
      }

      @Override
      public Set<String> noExportUntil() {
        Set<String> noExportUntil = new HashSet<>();
        noExportUntil.add("1");
        noExportUntil.add("2");
        return noExportUntil;
      }
    });

    OpenTelemetrySpanExporter openTelemetrySpanExporter = OpenTelemetrySpanExporter
        .builder()
        .withInternalSpan(internalSpan)
        .withStartSpanInfo(initialSpanInfo)
        .withArtifactId(ARTIFACT_ID)
        .withArtifactType(ARTIFACT_TYPE)
        .build();

    assertThat(openTelemetrySpanExporter.getOpenTelemetrySpan(), instanceOf(expectedExportableClass));
    assertThat(openTelemetrySpanExporter.getOpenTelemetrySpan().onlyPropagateNamesAndAttributes(),
               equalTo(isPolicy || !exportable));
    assertThat(openTelemetrySpanExporter.getOpenTelemetrySpan().isRoot(), equalTo(isRoot && exportable));
    if (exportable) {
      assertThat(openTelemetrySpanExporter.getOpenTelemetrySpan().getNoExportUntil(), hasItems("1", "2"));
    }
  }
}
