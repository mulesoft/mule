/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.source.trace;

import static java.util.Collections.emptyMap;

import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.span.SpanAttribute;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An implementation for {@link DistributedTraceContextManager} used internally for sources.
 *
 * @since 4.5.0
 */
public class SourceDistributedTraceContextManager implements DistributedTraceContextManager, SpanContextAware {

  private Map<String, String> remoteTraceContextMap = emptyMap();
  private String name;
  private final List<SpanAttribute<String>> attributes = new ArrayList<>();
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
    // Mejorar esto (por ahi tener en API un StringSpanAttribute)
    attributes.add(new SpanAttribute<String>() {

      @Override
      public String getKey() {
        return key;
      }

      @Override
      public String getValue() {
        return value;
      }
    });
  }

  @Override
  public void addCurrentSpanAttributes(Map<String, String> attributes) {
    attributes.forEach(this::addCurrentSpanAttribute);
  }

  public List<SpanAttribute<String>> getSpanRootAttributes() {
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
