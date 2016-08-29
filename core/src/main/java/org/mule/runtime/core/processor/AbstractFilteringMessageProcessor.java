/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.FilterUnacceptedException;
import org.mule.runtime.core.config.i18n.CoreMessages;

/**
 * Abstract {@link InterceptingMessageProcessor} that can be easily be extended and used for filtering message flow through a
 * {@link MessageProcessor} chain. The default behaviour when the filter is not accepted is to return the request event.
 */
public abstract class AbstractFilteringMessageProcessor extends AbstractInterceptingMessageProcessor
    implements NonBlockingSupported {

  /**
   * Throw a FilterUnacceptedException when a message is rejected by the filter?
   */
  protected boolean throwOnUnaccepted = false;
  protected boolean onUnacceptedFlowConstruct;


  /**
   * The <code>MessageProcessor</code> that should be used to handle messages that are not accepted by the filter.
   */
  protected MessageProcessor unacceptedMessageProcessor;

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    boolean accepted;
    try {
      accepted = accept(event);
    } catch (Exception ex) {
      throw filterFailureException(event, ex);
    }
    if (accepted) {
      return processNext(event);
    } else {
      return handleUnaccepted(event);
    }
  }

  protected abstract boolean accept(MuleEvent event);

  protected MuleEvent handleUnaccepted(MuleEvent event) throws MuleException {
    if (unacceptedMessageProcessor != null) {
      return unacceptedMessageProcessor.process(event);
    } else if (throwOnUnaccepted) {
      throw filterUnacceptedException(event);
    } else {
      return null;
    }
  }

  protected MessagingException filterFailureException(MuleEvent event, Exception ex) {
    return new MessagingException(event, ex, this);
  }

  protected MuleException filterUnacceptedException(MuleEvent event) {
    return new FilterUnacceptedException(CoreMessages.messageRejectedByFilter(), event, this);
  }

  public MessageProcessor getUnacceptedMessageProcessor() {
    return unacceptedMessageProcessor;
  }

  public void setUnacceptedMessageProcessor(MessageProcessor unacceptedMessageProcessor) {
    this.unacceptedMessageProcessor = unacceptedMessageProcessor;
    if (unacceptedMessageProcessor instanceof FlowConstruct) {
      onUnacceptedFlowConstruct = true;
    }
  }

  public boolean isThrowOnUnaccepted() {
    return throwOnUnaccepted;
  }

  public void setThrowOnUnaccepted(boolean throwOnUnaccepted) {
    this.throwOnUnaccepted = throwOnUnaccepted;
  }
}
