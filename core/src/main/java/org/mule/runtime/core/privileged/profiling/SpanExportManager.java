/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.profiling;

import static java.util.Collections.emptySet;

import java.util.Collection;

/**
 * A manager for the export of {@link org.mule.runtime.api.profiling.tracing.Span}
 *
 * @since 4.5.0
 */
public interface SpanExportManager {

  /**
   * @return gets an {@link ExportedSpanCapturer}.
   *
   *         This is used for capturing spans in privileged modules but should not be exposed as API.
   *
   * @since 4.5.0
   */
  default ExportedSpanCapturer getExportedSpanCapturer() {
    return new ExportedSpanCapturer() {

      @Override
      public Collection<CapturedExportedSpan> getExportedSpans() {
        return emptySet();
      }

      @Override
      public void dispose() {
        // Nothing to dispose.
      }
    };
  }

}
