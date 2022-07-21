/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.profiling;

/**
 * Encapsulates data corresponding to a captured exported span. This is only used for testing purposes and is not exposed as
 * general API.
 *
 * @see ExportedSpanCapturer
 *
 * @since 4.5.0
 */
public interface CapturedExportedSpan {

  /**
   * @return the name of the exported span.
   */
  String getName();

  /**
   * @return the paranet span id.
   */
  String getParentSpanId();

  /**
   * @return the span id.
   */
  String getSpanId();
}
