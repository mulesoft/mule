/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.requestreply;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.RequestReplyReplierMessageProcessor;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;

public class SimpleRequestReplyReplier extends AbstractInterceptingMessageProcessor
    implements RequestReplyReplierMessageProcessor {

  protected Processor replyMessageProcessor;

  public Event process(Event event) throws MuleException {
    replyMessageProcessor.process(processNext(event));
    return event;
  }

  public void setReplyProcessor(Processor replyMessageProcessor) {
    this.replyMessageProcessor = replyMessageProcessor;
  }

}
