/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.privileged.profiling.SpanExportManager;

/**
 * An internal {@link SpanExportManager}.
 *
 * @since 4.5.0
 */
public interface InternalSpanExportManager<T> extends SpanExportManager {

  /**
   * @param context                           the context
   * @param muleConfiguration                 information about the artifact where the span is generated.
   * @param exportUntilLevel                  Indicates the level until which the span hierarchy will be exported.
   * @param internalSpan                      the {@link InternalSpan} to export.
   * @param ignoreExportLevelLimitOfAncestors ignoring if one of the ancestors has set the export until level.
   * @return an {@link InternalSpanExporter}.
   */
  InternalSpanExporter getInternalSpanExporter(T context, MuleConfiguration muleConfiguration, boolean exportable,
                                               int exportUntilLevel,
                                               boolean ignoreExportLevelLimitOfAncestors,
                                               InternalSpan internalSpan);
}
