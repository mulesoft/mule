/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source.trace;

import static java.util.Collections.emptyMap;

import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation for {@link DistributedTraceContextManager} used internally for sources.
 *
 * @since 4.5.0
 */
public class SourceDistributedTraceContextManager implements DistributedTraceContextManager, SpanContextAware {

  private Map<String, String> remoteTraceContextMap = emptyMap();
  private String name;
  private Map<String, String> attributes = new HashMap<>();
  private SpanContext spanContext;

  @Override
  public void setRemoteTraceContextMap(Map<String, String> remoteTraceContextMap) {
    this.remoteTraceContextMap = remoteTraceContextMap;
  }

  @Override
  public Map<String, String> getRemoteTraceContextMap() {
    return remoteTraceContextMap;
  }

  @Override
  public void setCurrentSpanName(String name) {
    this.name = name;
  }

  @Override
  public void addCurrentSpanAttribute(String key, String value) {
    if (spanContext != null) {
      spanContext.getSpan().ifPresent(span -> span.addAttribute(key, value));
    }
    attributes.put(key, value);
  }

  @Override
  public void addCurrentSpanAttributes(Map<String, String> attributes) {
    attributes.forEach(this::addCurrentSpanAttribute);
  }

  public Map<String, String> getSpanRootAttributes() {
    return attributes;
  }

  public String getSpanName() {
    return name;
  }

  @Override
  public SpanContext getSpanContext() {
    return spanContext;
  }

  @Override
  public void setSpanContext(SpanContext spanContext) {
    this.spanContext = spanContext;
  }
}
