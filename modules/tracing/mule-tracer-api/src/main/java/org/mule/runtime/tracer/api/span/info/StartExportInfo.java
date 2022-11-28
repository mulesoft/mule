/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.api.span.info;

import org.mule.runtime.api.event.Event;

import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * The export information when starting a {@link org.mule.runtime.tracer.api.span.InternalSpan}.
 *
 * @see org.mule.runtime.tracer.api.EventTracer#startComponentSpan(Event, InitialSpanInfo)
 *
 * @since 4.5.0
 */
public interface StartExportInfo {

  StartExportInfo DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO = new StartExportInfo() {

    @Override
    public boolean isExportable() {
      return true;
    }
  };

  StartExportInfo NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO = new StartExportInfo() {

    @Override
    public boolean isExportable() {
      return false;
    }
  };

  /**
   * @return if the span should be exported.
   */
  default boolean isExportable() {
    return true;
  }

  /**
   * Indicates that no children will be exported till a span is found with the names returned.
   * <p>
   * For example: in case noExportUntil returns "execute-next", no children will be exported till an execute-next span.
   * <p>
   * ------------- span (exported) --------------------------------------------------------- |___ logger (not exported) ____ |___
   * scope (not exported) |___ execute-next (exported) |__ flow (exported)
   *
   * @return the name of the spans where the span hierarchy is exported again.
   */
  default Set<String> noExportUntil() {
    return emptySet();
  }
}
