/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.impl;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import org.mule.runtime.metrics.api.meter.Meter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

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
