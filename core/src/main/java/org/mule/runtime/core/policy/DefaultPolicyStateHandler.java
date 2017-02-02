/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.Optional.ofNullable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.map.MultiValueMap;

public class DefaultPolicyStateHandler implements PolicyStateHandler {

  private MultiValueMap policyStateIdsByExecutionIdentifier = new MultiValueMap();
  private Map<PolicyStateId, Event> stateMap = new HashMap<>();
  private Map<String, Processor> nextOperationMap = new HashMap<>();

  public void updateNextOperation(String identifier, Processor nextOperation) {
    nextOperationMap.put(identifier, nextOperation);
  }

  public Processor retrieveNextOperation(String identifier) {
    return nextOperationMap.get(identifier);
  }

  public Optional<Event> getLatestState(PolicyStateId identifier) {
    return ofNullable(stateMap.get(identifier));
  }

  public void updateState(PolicyStateId identifier, Event lastStateEvent) {
    stateMap.put(identifier, lastStateEvent);
    policyStateIdsByExecutionIdentifier.put(identifier.getExecutionIndentifier(), identifier);
  }

  public void destroyState(String identifier) {
    Collection<PolicyStateId> policyStateIds = policyStateIdsByExecutionIdentifier.getCollection(identifier);
    if (policyStateIds != null) {
      policyStateIds.stream().forEach(stateMap::remove);
    }
    policyStateIdsByExecutionIdentifier.remove(identifier);
    nextOperationMap.remove(identifier);
  }

}
