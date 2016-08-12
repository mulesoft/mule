/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.message.Correlation;

import java.util.ArrayList;
import java.util.List;

/**
 * A router that breaks up the current message onto smaller parts and sends them to the same destination. The Destination service
 * needs to have a MessageChunkingAggregator inbound router in order to rebuild the message at the other end.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www.eaipatterns.com/Sequencer.html</a>
 */
public class MessageChunkSplitter extends AbstractSplitter {

  protected int messageSize = 0;

  public int getMessageSize() {
    return messageSize;
  }

  public void setMessageSize(int messageSize) {
    this.messageSize = messageSize;
  }

  @Override
  protected boolean isSplitRequired(MuleEvent event) {
    return messageSize != 0;
  }

  @Override
  protected List<MuleEvent> splitMessage(MuleEvent event) throws RoutingException {
    List<MuleEvent> messageParts = new ArrayList<>();
    byte[] data;
    try {
      data = event.getMessageAsBytes();
    } catch (Exception e) {
      throw new RoutingException(CoreMessages.failedToReadPayload(), event, next, e);
    }

    MuleMessage message = event.getMessage();
    int parts = data.length / messageSize;
    if ((parts * messageSize) < data.length) {
      parts++;
    }
    int len = messageSize;
    int count = 0;
    int pos = 0;
    byte[] buffer;
    for (; count < parts; count++) {
      if ((pos + len) > data.length) {
        len = data.length - pos;
      }
      buffer = new byte[len];
      System.arraycopy(data, pos, buffer, 0, buffer.length);
      pos += len;
      final DefaultMuleEvent childEvent = new DefaultMuleEvent(MuleMessage.builder(message).payload(buffer).build(), event);
      childEvent.setParent(event);
      childEvent.setCorrelation(new Correlation(event.getExecutionContext().getSourceCorrelationId().orElse(null), parts, count));

      messageParts.add(childEvent);
    }
    return messageParts;
  }

}
