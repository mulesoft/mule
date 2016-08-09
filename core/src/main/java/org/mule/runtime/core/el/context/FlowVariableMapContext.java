/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import org.mule.runtime.core.api.MuleEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Flow variables wrapper {@link Map} for exposing Flow variables via an {@link org.mule.runtime.core.api.el.ExpressionLanguage}
 */
public class FlowVariableMapContext extends AbstractMapContext<Object> {

  private MuleEvent event;

  public FlowVariableMapContext(MuleEvent event) {
    this.event = event;
  }

  @Override
  public Object doGet(String key) {
    return event.getFlowVariable(key);
  }

  @Override
  public void doPut(String key, Object value) {
    event.setFlowVariable(key, value);
  }

  @Override
  public void doRemove(String key) {
    event.removeFlowVariable(key);
  }

  @Override
  public Set<String> keySet() {
    return event.getFlowVariableNames();
  }

  @Override
  public void clear() {
    event.clearFlowVariables();
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
