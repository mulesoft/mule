/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static java.util.Collections.emptyMap;
import org.mule.runtime.core.api.MuleEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Flow variables wrapper {@link Map} for exposing Flow variables via an {@link org.mule.runtime.core.api.el.ExpressionLanguage}
 */
public class FlowVariableMapContext extends AbstractMapContext<Object> {

  private MuleEvent event;
  private MuleEvent.Builder eventBuider;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public FlowVariableMapContext(MuleEvent event, MuleEvent.Builder eventBuider) {
    this.event = event;
    this.eventBuider = eventBuider;
  }

  @Override
  public Object doGet(String key) {
    return event.getFlowVariable(key);
  }

  @Override
  public void doPut(String key, Object value) {
    eventBuider.addFlowVariable(key, value);
    event = eventBuider.build();
  }

  @Override
  public void doRemove(String key) {
    eventBuider.removeFlowVariable(key);
    event = eventBuider.build();
  }

  @Override
  public Set<String> keySet() {
    return event.getFlowVariableNames();
  }

  @Override
  public void clear() {
    eventBuider.flowVariables(emptyMap());
    event = eventBuider.build();
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<String, Object>();
    for (String key : event.getFlowVariableNames()) {
      Object value = event.getFlowVariable(key);
      map.put(key, value);
    }
    return map.toString();
  }
}
