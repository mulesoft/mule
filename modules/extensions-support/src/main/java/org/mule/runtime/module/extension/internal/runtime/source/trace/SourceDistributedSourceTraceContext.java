/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.source.trace;

import static java.util.Collections.emptyMap;

import org.mule.runtime.core.api.event.CoreEvent;
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
public class SourceDistributedSourceTraceContext implements DistributedTraceContextManager, SpanContextAware {

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
    attributes.put(key, value);
  }

  @Override
  public void addCurrentSpanAttributes(Map<String, String> attributes) {
    attributes.putAll(attributes);
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
