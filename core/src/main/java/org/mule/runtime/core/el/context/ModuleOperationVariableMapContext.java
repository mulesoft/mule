/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.api.Event.getVariableValueOrNull;
import org.mule.runtime.core.api.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//TODO until MULE-10291 & MULE-10353 are done, we will use flowVars to store the parameter.value and property.value
public class ModuleOperationVariableMapContext extends AbstractMapContext<Object> {

  private Event event;
  private Event.Builder eventBuider;
  private String prefix;

  public ModuleOperationVariableMapContext(Event event, Event.Builder eventBuider, String prefix) {
    this.event = event;
    this.eventBuider = eventBuider;
    this.prefix = prefix;
  }

  @Override
  public Object doGet(String key) {
    return getVariableValueOrNull(applyPrefix(key), event);
  }

  @Override
  public void doPut(String key, Object value) {
    eventBuider.addVariable(applyPrefix(key), value);
    event = eventBuider.build();
  }

  @Override
  public void doRemove(String key) {
    eventBuider.removeVariable(applyPrefix(key));
    event = eventBuider.build();
  }

  @Override
  public Set<String> keySet() {
    return event.getVariableNames();
  }

  @Override
  public void clear() {
    eventBuider.variables(emptyMap());
    event = eventBuider.build();
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    for (String key : event.getVariableNames()) {
      String keyWithPrefix = applyPrefix(key);
      Object value = event.getVariable(keyWithPrefix) != null ? event.getVariable(keyWithPrefix).getValue() : null;
      map.put(keyWithPrefix, value);
    }
    return map.toString();
  }

  private String applyPrefix(String key) {
    return prefix + "." + key;
  }
}
