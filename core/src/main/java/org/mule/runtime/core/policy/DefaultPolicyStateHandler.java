/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.Optional.of;
import org.mule.runtime.core.api.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultPolicyStateHandler implements PolicyStateHandler {

  private Map<String, Event> stateMap = new HashMap<>();
  private Map<String, NextOperation> nextOperationMap = new HashMap<>();

  public void updateNextOperation(String identifier, NextOperation nextOperation) {
    nextOperationMap.put(identifier, nextOperation);
  }

  public NextOperation retrieveNextOperation(String identifier) {
    return nextOperationMap.get(identifier);
  }

  public Optional<Event> getLatestState(String identifier) {
    return of(this.stateMap.get(identifier));
  }

  public void updateState(String identifier, Event lastStateEvent) {
    this.stateMap.put(identifier, lastStateEvent);
  }

  public void destroyState(String identifier) {
    stateMap.remove(identifier);
    nextOperationMap.remove(identifier);
  }

}
