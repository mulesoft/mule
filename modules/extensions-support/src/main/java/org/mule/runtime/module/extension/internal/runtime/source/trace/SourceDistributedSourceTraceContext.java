/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.source.trace;

import static java.util.Collections.emptyMap;

import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.Map;

/**
 * An implementation for {@link DistributedTraceContextManager} used internally for sources.
 *
 * @since 4.5.0
 */
public class SourceDistributedSourceTraceContext implements DistributedTraceContextManager {

  private Map<String, String> remoteTraceContextMap = emptyMap();

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
    // Nothing to do in sources.
  }

  @Override
  public void addCurrentSpanAttribute(String key, String value) {
    // Nothing to do in sources.
  }

  @Override
  public void addCurrentSpanAttributes(Map<String, String> attributes) {
    // Nothing to do in sources.
  }
}
