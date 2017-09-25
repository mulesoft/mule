/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing.requestreply;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.AbstractInterceptingMessageProcessor;

public abstract class AbstractReplyToPropertyRequestReplyReplier extends AbstractInterceptingMessageProcessor
    implements RequestReplyReplierMessageProcessor, InternalProcessor {

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    CoreEvent resultEvent;
    PrivilegedEvent privilegedEvent = (PrivilegedEvent) event;
    if (shouldProcessEvent(privilegedEvent)) {
      Object replyTo = privilegedEvent.getReplyToDestination();
      ReplyToHandler replyToHandler = privilegedEvent.getReplyToHandler();

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

  protected abstract boolean shouldProcessEvent(PrivilegedEvent event);

  protected CoreEvent processReplyTo(CoreEvent event, CoreEvent result, ReplyToHandler replyToHandler, Object replyTo)
      throws MuleException {
    if (result != null && replyToHandler != null) {
      String requestor = ((InternalMessage) result.getMessage()).getOutboundProperty(MULE_REPLY_TO_REQUESTOR_PROPERTY);
      if ((requestor != null && !requestor.equals(getLocation().getRootContainerName())) || requestor == null) {
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
