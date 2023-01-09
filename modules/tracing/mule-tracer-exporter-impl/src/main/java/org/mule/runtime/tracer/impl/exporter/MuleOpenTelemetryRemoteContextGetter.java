/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import java.util.Map;
import javax.annotation.Nullable;

import io.opentelemetry.context.propagation.TextMapGetter;

/**
 * An Internal {@link TextMapGetter} to retrieve the remote span context.
 * <p>
 * This is used to resolve a remote OpTel Span propagated through W3C Trace Context.
 *
 * @since 4.5.0
 */
public class MuleOpenTelemetryRemoteContextGetter implements TextMapGetter<Map<String, String>> {

  @Override
  public Iterable<String> keys(Map<String, String> stringStringMap) {
    return stringStringMap.keySet();
  }

  @Nullable
  @Override
  public String get(@Nullable Map<String, String> stringStringMap, @Nullable String key) {
    if (stringStringMap == null) {
      return null;
    }

    return stringStringMap.get(key);
  }

}
