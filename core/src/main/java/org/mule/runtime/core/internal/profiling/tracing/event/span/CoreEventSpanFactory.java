/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import static java.util.Collections.emptyList;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizer;

import java.util.Collection;

/**
 * Provides the Span from the core event and the component.
 *
 * @since 4.5.0
 */
public interface CoreEventSpanFactory {

  /**
   * Provides a span for related to a component that is hit by an event.
   *
   * @param coreEvent         the core event that hits the component.
   * @param muleConfiguration the mule configuration related to the deployed artifact.
   * @param artifactType      the artifact type.
   * @param spanCustomizer    the {@link SpanCustomizer} customizer for the span.
   * @return the {@link InternalSpan} for that coreEvent and component.
   */
  InternalSpan getSpan(CoreEvent coreEvent, MuleConfiguration muleConfiguration,
                       ArtifactType artifactType,
                       SpanCustomizer spanCustomizer);

  /**
   * @return a capturer for created spans that are exported.
   */
  default ExportedSpanCapturer getExportedSpanCapturer() {
    return new ExportedSpanCapturer() {

      @Override
      public Collection<CapturedExportedSpan> getExportedSpans() {
        return emptyList();
      }

      @Override
      public void dispose() {
        // Nothing to dispose
      }
    };
  }
}
