/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.context;

import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.el.context.AbstractMapContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InboundPropertiesMapContext extends AbstractMapContext<Serializable> {

  private CoreEvent event;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public InboundPropertiesMapContext(CoreEvent event) {
    this.event = event;
  }

  @Override
  public Serializable doGet(String key) {
    return ((InternalMessage) event.getMessage()).getInboundProperty(key);
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
    return ((InternalMessage) event.getMessage()).getInboundPropertyNames();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(CoreMessages.inboundMessagePropertiesImmutable().getMessage());
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    for (String key : ((InternalMessage) event.getMessage()).getInboundPropertyNames()) {
      Object value = ((InternalMessage) event.getMessage()).getInboundProperty(key);
      map.put(key, value);
    }
    return map.toString();
  }
}
