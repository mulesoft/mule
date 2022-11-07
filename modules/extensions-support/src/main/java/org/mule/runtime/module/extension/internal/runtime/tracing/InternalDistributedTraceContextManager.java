/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.tracing;

import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.Map;

/**
 * Internal interface that allows to retrieve information set by a connector.
 *
 * @since 4.5.0
 */
public interface InternalDistributedTraceContextManager extends DistributedTraceContextManager {

  static InternalDistributedTraceContextManager getInternalDistributedTraceContextManager(DistributedTraceContextManager distributedSourceTraceContext) {
    if (distributedSourceTraceContext instanceof InternalDistributedTraceContextManager) {
      return (InternalDistributedTraceContextManager) distributedSourceTraceContext;
    }

    return new InternalDistributedTraceContextManagerWrapper(distributedSourceTraceContext);
  }

  <T> T visit(InternalDistributedTraceContextVisitor<T> visitor);

  static class InternalDistributedTraceContextManagerWrapper implements InternalDistributedTraceContextManager {

    private final DistributedTraceContextManager distributedSourceTraceContext;

    public InternalDistributedTraceContextManagerWrapper(
                                                         DistributedTraceContextManager distributedSourceTraceContext) {
      this.distributedSourceTraceContext = distributedSourceTraceContext;
    }

    @Override
    public void setRemoteTraceContextMap(Map<String, String> contextMap) {
      distributedSourceTraceContext.setRemoteTraceContextMap(contextMap);
    }

    @Override
    public Map<String, String> getRemoteTraceContextMap() {
      return distributedSourceTraceContext.getRemoteTraceContextMap();
    }

    @Override
    public void setCurrentSpanName(String name) {
      distributedSourceTraceContext.setCurrentSpanName(name);
    }

    @Override
    public void addCurrentSpanAttribute(String key, String value) {
      distributedSourceTraceContext.addCurrentSpanAttribute(key, value);
    }

    @Override
    public void addCurrentSpanAttributes(Map<String, String> attributes) {
      distributedSourceTraceContext.addCurrentSpanAttributes(attributes);
    }

    @Override
    public <T> T visit(InternalDistributedTraceContextVisitor<T> visitor) {
      return null;
    }
  }
}

