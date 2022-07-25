/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.export;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;

/**
 * A configuration for the export of {@link InternalSpan}'s
 *
 * @since 4.5.0
 */
public interface SpanExporterConfiguration {

  /**
   * @param key the key of a configuration parameter
   * @return the value associated to the {@param key}
   */
  String getValue(String key);

}
