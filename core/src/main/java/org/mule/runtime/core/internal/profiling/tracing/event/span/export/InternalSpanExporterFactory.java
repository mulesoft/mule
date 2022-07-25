/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

/**
 * A factory for a {@link InternalSpanExporter}
 *
 * @since 4.5.0
 */
public interface InternalSpanExporterFactory<T> {

  /**
   * @param context      an extra instance that may have extra information for creat
   *
   * @param internalSpan the {@link InternalSpan} that will eventually be exported
   * @return the result exporter.
   */
  InternalSpanExporter from(T context, InternalSpan internalSpan);

  /**
   * @return returns a {@link ExportedSpanCapturer}.
   */
  ExportedSpanCapturer getExportedSpanCapturer();
}
