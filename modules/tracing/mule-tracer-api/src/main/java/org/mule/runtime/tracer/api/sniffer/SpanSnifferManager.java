/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.api.sniffer;

import static java.util.Collections.emptySet;

import java.util.Collection;

/**
 * A manager for the sniffing of exported {@link org.mule.runtime.api.profiling.tracing.Span}
 *
 * @since 4.5.0
 */
public interface SpanSnifferManager {

  /**
   * @return gets an {@link ExportedSpanSniffer}.
   *
   *         This is used for capturing spans in privileged modules but should not be exposed as API.
   *
   * @since 4.5.0
   */
  default ExportedSpanSniffer getExportedSpanSniffer() {
    return new ExportedSpanSniffer() {

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
