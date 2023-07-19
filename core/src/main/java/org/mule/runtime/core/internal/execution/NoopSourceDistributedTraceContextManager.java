/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Collections.emptyMap;

import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.Map;

/**
 * A distributed trace context manager that returns an empty remote context and does not do anything when setting the span name or
 * attributes. It may be used in cases where there was no propagation of distributed trace context from the source. For example,
 * in schedulers.
 *
 * @see SourceResultAdapter
 *
 * @since 4.5.0.
 */
public class NoopSourceDistributedTraceContextManager implements DistributedTraceContextManager {

  private static final DistributedTraceContextManager INSTANCE = new NoopSourceDistributedTraceContextManager();

  private NoopSourceDistributedTraceContextManager() {

  }

  public static DistributedTraceContextManager getNoopSourceDistributedTraceContextManager() {
    return INSTANCE;
  }

  @Override
  public void setRemoteTraceContextMap(Map<String, String> map) {
    // Nothing to do.
  }

  @Override
  public Map<String, String> getRemoteTraceContextMap() {
    return emptyMap();
  }

  @Override
  public void setCurrentSpanName(String spanName) {
    // Nothing to do.
  }

  @Override
  public void addCurrentSpanAttribute(String key, String value) {
    // Nothign to do
  }

  @Override
  public void addCurrentSpanAttributes(Map<String, String> attributes) {
    // Nothing to do.
  }
}
