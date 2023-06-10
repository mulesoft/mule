/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import org.mule.runtime.metrics.api.meter.Meter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * {@link Attributes} based on a {@link Meter} for otel export.
 */
public class OpentelemetryExporterAttributes implements Attributes {

  private Map<AttributeKey<?>, String> attributesToExport = new HashMap<>();

  public OpentelemetryExporterAttributes(Meter meter) {
    meter.forEachAttribute((key, value) -> attributesToExport.put(stringKey(key), value));
  }

  @Override
  public <T> T get(AttributeKey<T> key) {
    return (T) attributesToExport.get(key);
  }

  @Override
  public void forEach(BiConsumer<? super AttributeKey<?>, ? super Object> consumer) {
    attributesToExport.forEach(consumer::accept);
  }

  @Override
  public int size() {
    return attributesToExport.size();
  }

  @Override
  public boolean isEmpty() {
    return attributesToExport.isEmpty();
  }

  @Override
  public Map<AttributeKey<?>, Object> asMap() {
    Map<AttributeKey<?>, Object> map = new HashMap<>();
    attributesToExport.forEach(map::put);
    return map;
  }

  @Override
  public AttributesBuilder toBuilder() {
    throw new UnsupportedOperationException();
  }
}
