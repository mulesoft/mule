/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl;

import java.util.Map;

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

  @Override
  public String get(Map<String, String> stringStringMap, String key) {
    if (stringStringMap == null) {
      return null;
    }

    return stringStringMap.get(key);
  }

}
