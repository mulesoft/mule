/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.PolicyStateId;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultPolicyStateHandler implements PolicyStateHandler {

  protected MultiMap<String, PolicyStateId> policyStateIdsByExecutionIdentifier = new MultiMap<>();
  protected Map<PolicyStateId, CoreEvent> stateMap = new HashMap<>();
  protected Map<String, Processor> nextOperationMap = new HashMap<>();

  public synchronized void updateNextOperation(String identifier, Processor nextOperation) {
    nextOperationMap.put(identifier, nextOperation);
  }

  public synchronized Processor retrieveNextOperation(String identifier) {
    return nextOperationMap.get(identifier);
  }

  public Optional<CoreEvent> getLatestState(PolicyStateId identifier) {
    final CoreEvent state;
    synchronized (this) {
      state = stateMap.get(identifier);
    }
    return ofNullable(state);
  }

  public void updateState(PolicyStateId identifier, CoreEvent lastStateEvent) {
    ((BaseEventContext) lastStateEvent.getContext()).getRootContext()
        .onTerminated((response, throwable) -> destroyState(identifier.getExecutionIdentifier()));
    synchronized (this) {
      stateMap.put(identifier, lastStateEvent);
      policyStateIdsByExecutionIdentifier.put(identifier.getExecutionIdentifier(), identifier);
    }
  }

  public synchronized void destroyState(String identifier) {
    List<PolicyStateId> policyStateIds = policyStateIdsByExecutionIdentifier.removeAll(identifier);
    if (policyStateIds != null) {
      policyStateIds.forEach(stateMap::remove);
    }
    nextOperationMap.remove(identifier);
  }

}
