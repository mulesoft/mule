/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.requestreply;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.RequestReplyReplierMessageProcessor;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;

public abstract class AbstractReplyToPropertyRequestReplyReplier extends AbstractInterceptingMessageProcessor
    implements RequestReplyReplierMessageProcessor, InternalMessageProcessor {

  @Override
  public Event process(Event event) throws MuleException {
    Event resultEvent;
    if (shouldProcessEvent(event)) {
      Object replyTo = event.getReplyToDestination();
      ReplyToHandler replyToHandler = event.getReplyToHandler();

      resultEvent = processNext(event);

      // Allow components to stop processing of the ReplyTo property (e.g. CXF)
      if (resultEvent != null) {
        // reply-to processing should not resurrect a dead event
        event = processReplyTo(event, resultEvent, replyToHandler, replyTo);
      }
    } else {
      resultEvent = processNext(event);
    }
    return resultEvent;
  }

  protected abstract boolean shouldProcessEvent(Event event);

  protected Event processReplyTo(Event event, Event result, ReplyToHandler replyToHandler, Object replyTo)
      throws MuleException {
    if (result != null && replyToHandler != null) {
      String requestor = result.getMessage().getOutboundProperty(MULE_REPLY_TO_REQUESTOR_PROPERTY);
      if ((requestor != null && !requestor.equals(flowConstruct.getName())) || requestor == null) {
        return replyToHandler.processReplyTo(event, result.getMessage(), replyTo);
      } else {
        return event;
      }
    } else {
      return event;
    }
  }

  @Override
  public void setReplyProcessor(Processor replyMessageProcessor) {
    // Not used
  }

}
