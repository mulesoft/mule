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

import java.util.Set;

import javax.activation.DataHandler;

public class OutboundAttachmentMapContext extends AbstractMapContext<DataHandler> {

  private MuleEvent event;
  private MuleEvent.Builder eventBuilder;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public OutboundAttachmentMapContext(MuleEvent event, MuleEvent.Builder eventBuilder) {
    this.event = event;
    this.eventBuilder = eventBuilder;
  }

  @Override
  public DataHandler doGet(String key) {
    return event.getMessage().getOutboundAttachment(key);
  }

  @Override
  public void doPut(String key, DataHandler value) {
    eventBuilder.message(MuleMessage.builder(event.getMessage()).addOutboundAttachment(key, value).build());
    event = eventBuilder.build();
  }

  @Override
  public void doRemove(String key) {
    eventBuilder.message(MuleMessage.builder(event.getMessage()).removeOutboundAttachment(key).build());
    event = eventBuilder.build();
  }

  @Override
  public Set<String> keySet() {
    return event.getMessage().getOutboundAttachmentNames();
  }

  @Override
  public void clear() {
    eventBuilder.message(MuleMessage.builder(event.getMessage()).outboundAttachments(emptyMap()).build());
    event = eventBuilder.build();
  }

}
