/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.context;

import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.el.context.AbstractMapContext;

import java.util.Set;

import javax.activation.DataHandler;

public class InboundAttachmentMapContext extends AbstractMapContext<DataHandler> {

  private CoreEvent event;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public InboundAttachmentMapContext(CoreEvent event) {
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
