/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.parameter;

import static java.util.Collections.unmodifiableMap;

import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link DistributedTraceContextManager} that injects all the fields of the propagator context of the event.
 *
 * @since 4.5.0
 */
public class PropagateAllDistributedTraceContextManager implements DistributedTraceContextManager {

  private Map<String, String> contextMap;

  public PropagateAllDistributedTraceContextManager(DistributedTraceContext distributedTraceContext) {
    resolveContextMap(distributedTraceContext);
  }

  private void resolveContextMap(DistributedTraceContext distributedTraceContext) {
    Map<String, String> contextMapToBuild = new HashMap<>(distributedTraceContext.tracingFieldsAsMap());
    contextMapToBuild.putAll(distributedTraceContext.baggageItemsAsMap());
    contextMap = unmodifiableMap(contextMapToBuild);
  }

  @Override
  public void setRemoteTraceContextMap(Map<String, String> contextMap) {
    this.contextMap = contextMap;
  }

  @Override
  public Map<String, String> getRemoteTraceContextMap() {
    return contextMap;
  }
}
