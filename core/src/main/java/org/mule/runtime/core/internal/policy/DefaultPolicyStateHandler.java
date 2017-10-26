/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.ofNullable;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.PolicyStateId;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.map.MultiValueMap;

public class DefaultPolicyStateHandler implements PolicyStateHandler {

  protected MultiValueMap policyStateIdsByExecutionIdentifier = new MultiValueMap();
  protected Map<PolicyStateId, CoreEvent> stateMap = new HashMap<>();
  protected Map<String, Processor> nextOperationMap = new HashMap<>();

  public void updateNextOperation(String identifier, Processor nextOperation) {
    nextOperationMap.put(identifier, nextOperation);
  }

  public Processor retrieveNextOperation(String identifier) {
    return nextOperationMap.get(identifier);
  }

  public Optional<CoreEvent> getLatestState(PolicyStateId identifier) {
    return ofNullable(stateMap.get(identifier));
  }


  public void updateState(PolicyStateId identifier, CoreEvent lastStateEvent) {
    from(((BaseEventContext) lastStateEvent.getContext()).getRootContext().getCompletionPublisher())
        .subscribe(null, null, () -> destroyState(identifier.getExecutionIdentifier()));
    stateMap.put(identifier, lastStateEvent);
    policyStateIdsByExecutionIdentifier.put(identifier.getExecutionIdentifier(), identifier);
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
