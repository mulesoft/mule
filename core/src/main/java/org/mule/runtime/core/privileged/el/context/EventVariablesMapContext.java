/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.el.context;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.api.event.CoreEvent.getVariableValueOrNull;

import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Flow variables wrapper {@link Map} for exposing Flow variables via an {@link ExtendedExpressionLanguageAdaptor}
 */
public class EventVariablesMapContext extends AbstractMapContext<Object> {

  private CoreEvent event;
  private CoreEvent.Builder eventBuider;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public EventVariablesMapContext(CoreEvent event, CoreEvent.Builder eventBuider) {
    this.event = event;
    this.eventBuider = eventBuider;
  }

  @Override
  public Object doGet(String key) {
    return getVariableValueOrNull(key, event);
  }

  @Override
  public void doPut(String key, Object value) {
    eventBuider.addVariable(key, value);
    event = eventBuider.build();
  }

  @Override
  public void doRemove(String key) {
    eventBuider.removeVariable(key);
    event = eventBuider.build();
  }

  @Override
  public Set<String> keySet() {
    return event.getVariables().keySet();
  }

  @Override
  public void clear() {
    eventBuider.variables(emptyMap());
    event = eventBuider.build();
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    event.getVariables().forEach((key, value) -> {
      Object object = value != null ? value.getValue() : null;
      map.put(key, object);
    });
    return map.toString();
  }
}
