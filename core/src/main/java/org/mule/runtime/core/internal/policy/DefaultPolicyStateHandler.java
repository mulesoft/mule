/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.google.common.collect.Multimaps.synchronizedMultimap;
import static java.util.Optional.ofNullable;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.PolicyStateId;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPolicyStateHandler implements PolicyStateHandler {

  protected Multimap<String, PolicyStateId> policyStateIdsByExecutionIdentifier =
      synchronizedMultimap(HashMultimap.<String, PolicyStateId>create());
  protected Map<PolicyStateId, CoreEvent> stateMap = new ConcurrentHashMap<>();
  protected Map<String, Processor> nextOperationMap = new ConcurrentHashMap<>();

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
    Collection<PolicyStateId> policyStateIds = policyStateIdsByExecutionIdentifier.get(identifier);
    if (policyStateIds != null) {
      policyStateIds.stream().forEach(stateMap::remove);
    }
    policyStateIdsByExecutionIdentifier.removeAll(identifier);
    nextOperationMap.remove(identifier);
  }

}
