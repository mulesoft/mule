package org.mule.runtime.tracer.impl.exporter;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;

import static java.lang.System.nanoTime;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.opentelemetry.api.trace.Span;
import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(PROFILING)
public class DecoratedMuleOpenTelemetrySpanTestCase extends AbstractMuleTestCase {

  @Test
  public void ifAttributeIsPresentSpanKindMustBeUpdated(){
    String spanKind = "TEST_CLIENT";

    InternalSpan internalSpan = mock(InternalSpan.class);
    when(internalSpan.getName()).thenReturn("test span");
    SpanDuration spanDuration = mock(SpanDuration.class);
    when(spanDuration.getStart()).thenReturn(nanoTime());
    when(internalSpan.getDuration()).thenReturn(spanDuration);
    Map<String, String> spanAttributes = new HashMap<>();
    spanAttributes.put(DecoratedMuleOpenTelemetrySpan.SPAN_KIND, spanKind);
    when(internalSpan.getAttributes()).thenReturn(spanAttributes);

    InitialSpanInfo initialSpanInfo = mock(InitialSpanInfo.class);
    when(initialSpanInfo.isPolicySpan()).thenReturn(false);
    when(initialSpanInfo.isRootSpan()).thenReturn(false);
    when(initialSpanInfo.getInitialExportInfo()).thenReturn(new InitialExportInfo() {

      @Override
      public boolean isExportable() {
        return true;
      }

      @Override
      public Set<String> noExportUntil() {
        Set<String> noExportUntil = new HashSet<>();
        noExportUntil.add("1");
        noExportUntil.add("2");
        return noExportUntil;
      }
    });

    DecoratedMuleOpenTelemetrySpan decoratedMuleOpenTelemetrySpan = new DecoratedMuleOpenTelemetrySpan((Span) internalSpan);

    decoratedMuleOpenTelemetrySpan.end(internalSpan, initialSpanInfo, "testArtifactId", "testArtifactType");
    
  }
}
