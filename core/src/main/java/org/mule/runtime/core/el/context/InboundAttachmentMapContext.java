/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.util.Set;

import javax.activation.DataHandler;

public class InboundAttachmentMapContext extends AbstractMapContext<DataHandler> {

  private Event event;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public InboundAttachmentMapContext(Event event) {
    this.event = event;
  }

  @Override
  public DataHandler doGet(String key) {
    return ((InternalMessage) event.getMessage()).getInboundAttachment(key);
  }

  @Override
  public void doPut(String key, DataHandler value) {
    throw new UnsupportedOperationException(CoreMessages.inboundMessageAttachmentsImmutable(key).getMessage());
  }

  @Override
  public void doRemove(String key) {
    throw new UnsupportedOperationException(CoreMessages.inboundMessageAttachmentsImmutable(key).getMessage());
  }

  @Override
  public Set<String> keySet() {
    return ((InternalMessage) event.getMessage()).getInboundAttachmentNames();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(CoreMessages.inboundMessageAttachmentsImmutable().getMessage());
  }
}
