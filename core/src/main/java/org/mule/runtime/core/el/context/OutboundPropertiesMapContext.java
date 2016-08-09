/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static java.util.Collections.emptyMap;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class OutboundPropertiesMapContext extends AbstractMapContext<Serializable> {

  private MuleEvent event;

  public OutboundPropertiesMapContext(MuleEvent event) {
    this.event = event;
  }

  @Override
  public Serializable doGet(String key) {
    return event.getMessage().getOutboundProperty(key);
  }

  @Override
  public void doPut(String key, Serializable value) {
    event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty(key, value).build());
  }

  @Override
  public void doRemove(String key) {
    event.setMessage(MuleMessage.builder(event.getMessage()).removeOutboundProperty(key).build());
  }

  @Override
  public Set<String> keySet() {
    return event.getMessage().getOutboundPropertyNames();
  }

  @Override
  public void clear() {
    event.setMessage(MuleMessage.builder(event.getMessage()).outboundProperties(emptyMap()).build());
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    for (String key : event.getMessage().getOutboundPropertyNames()) {
      Object value = event.getMessage().getOutboundProperty(key);
      map.put(key, value);
    }
    return map.toString();
  }
}
