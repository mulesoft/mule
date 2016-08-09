/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class InboundPropertiesMapContext extends AbstractMapContext<Serializable> {

  private MuleEvent event;

  public InboundPropertiesMapContext(MuleEvent event) {
    this.event = event;
  }

  @Override
  public Serializable doGet(String key) {
    return event.getMessage().getInboundProperty(key);
  }

  @Override
  public void doPut(String key, Serializable value) {
    throw new UnsupportedOperationException(CoreMessages.inboundMessagePropertiesImmutable(key).getMessage());
  }

  @Override
  public void doRemove(String key) {
    throw new UnsupportedOperationException(CoreMessages.inboundMessagePropertiesImmutable(key).getMessage());
  }

  @Override
  public Set<String> keySet() {
    return event.getMessage().getInboundPropertyNames();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(CoreMessages.inboundMessagePropertiesImmutable().getMessage());
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<String, Object>();
    for (String key : event.getMessage().getInboundPropertyNames()) {
      Object value = event.getMessage().getInboundProperty(key);
      map.put(key, value);
    }
    return map.toString();
  }
}
