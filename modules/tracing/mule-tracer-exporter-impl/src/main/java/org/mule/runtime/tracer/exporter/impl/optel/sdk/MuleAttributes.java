/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.sdk;

import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.ARTIFACT_ID;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.ARTIFACT_TYPE;
import static org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporterUtils.THREAD_END_NAME_KEY;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.exporter.impl.OpenTelemetrySpanExporter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

/**
 * An implementation of OTEL sdk {@link Attributes}.
 *
 * @since 4.5.0
 */
public class MuleAttributes implements Attributes {

  public static final int EXPORTER_ATTRIBUTES_BASE_SIZE = 3;

  private final OpenTelemetrySpanExporter openTelemetrySpanExporter;
  private final InternalSpan internalSpan;
  private final String artifactId;
  private final String artifactType;

  public MuleAttributes(OpenTelemetrySpanExporter openTelemetrySpanExporter,
                        String artifactId,
                        String artifactType) {
    this.openTelemetrySpanExporter = openTelemetrySpanExporter;
    this.internalSpan = openTelemetrySpanExporter.getInternalSpan();
    this.artifactId = artifactId;
    this.artifactType = artifactType;
  }

  @Override
  public <T> T get(AttributeKey<T> key) {
    throw new UnsupportedOperationException();
  }

  public void forEach(BiConsumer<? super AttributeKey<?>, ? super Object> biConsumer) {
    biConsumer.accept(ARTIFACT_ID, artifactId);
    biConsumer.accept(ARTIFACT_TYPE, artifactType);
    biConsumer.accept(THREAD_END_NAME_KEY, openTelemetrySpanExporter.getThreadEndName());
    internalSpan.forEachAttribute((key, value) -> biConsumer.accept(stringKey(key), value));
  }

  @Override
  public int size() {
    return EXPORTER_ATTRIBUTES_BASE_SIZE + internalSpan.getAttributesCount();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Map<AttributeKey<?>, Object> asMap() {
    Map<AttributeKey<?>, Object> attributes = new HashMap<>();
    forEach(attributes::put);
    return attributes;
  }

  @Override
  public AttributesBuilder toBuilder() {
    throw new UnsupportedOperationException();
  }
}
