/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.context;

import static java.util.Collections.emptyMap;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.el.context.AbstractMapContext;

import java.util.Set;

import javax.activation.DataHandler;

public class OutboundAttachmentMapContext extends AbstractMapContext<DataHandler> {

  private CoreEvent event;
  private CoreEvent.Builder eventBuilder;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public OutboundAttachmentMapContext(CoreEvent event, CoreEvent.Builder eventBuilder) {
    this.event = event;
    this.eventBuilder = eventBuilder;
  }

  @Override
  public DataHandler doGet(String key) {
    return ((InternalMessage) event.getMessage()).getOutboundAttachment(key);
  }

  @Override
  public void doPut(String key, DataHandler value) {
    eventBuilder.message(InternalMessage.builder(event.getMessage()).addOutboundAttachment(key, value).build());
    event = eventBuilder.build();
  }

  @Override
  public void doRemove(String key) {
    eventBuilder.message(InternalMessage.builder(event.getMessage()).removeOutboundAttachment(key).build());
    event = eventBuilder.build();
  }

  @Override
  public Set<String> keySet() {
    return ((InternalMessage) event.getMessage()).getOutboundAttachmentNames();
  }

  @Override
  public void clear() {
    eventBuilder.message(InternalMessage.builder(event.getMessage()).outboundAttachments(emptyMap()).build());
    event = eventBuilder.build();
  }

}
