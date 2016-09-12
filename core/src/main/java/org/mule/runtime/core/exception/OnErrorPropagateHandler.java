/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import java.util.ArrayList;
import java.util.List;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.Processor;

//TODO: MULE-9307 re-write junits for rollback exception strategy


/**
 * Handler that will propagate errors and rollback transactions. Replaces the rollback-exception-strategy from Mule 3.
 *
 * @since 4.0
 */
public class OnErrorPropagateHandler extends TemplateOnErrorHandler {

  private RedeliveryExceeded redeliveryExceeded;
  private Integer maxRedeliveryAttempts;


  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    LifecycleUtils.initialiseIfNeeded(redeliveryExceeded);
    super.doInitialise(muleContext);
  }

  public void setRedeliveryExceeded(RedeliveryExceeded redeliveryExceeded) {
    this.redeliveryExceeded = redeliveryExceeded;
  }

  public void setMaxRedeliveryAttempts(Integer maxRedeliveryAttempts) {
    this.maxRedeliveryAttempts = maxRedeliveryAttempts;
  }

  public Integer getMaxRedeliveryAttempts() {
    return maxRedeliveryAttempts;
  }

  public boolean hasMaxRedeliveryAttempts() {
    return this.maxRedeliveryAttempts != null;
  }

  @Override
  protected Event beforeRouting(MessagingException exception, Event event) {
    if (!isRedeliveryExhausted(exception)) {
      rollback(exception);
    }
    return event;
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    List<Processor> messageProcessors = new ArrayList<>(super.getMessageProcessors().size()
        + (redeliveryExceeded == null ? 0 : redeliveryExceeded.getMessageProcessors().size()));
    messageProcessors.addAll(super.getMessageProcessors());
    if (redeliveryExceeded != null) {
      messageProcessors.addAll(redeliveryExceeded.getMessageProcessors());
    }
    return messageProcessors;
  }

  private boolean isRedeliveryExhausted(Exception exception) {
    return (exception instanceof MessageRedeliveredException);
  }

  @Override
  protected Event route(Event event, MessagingException t) throws MessagingException {
    Event resultEvent = event;
    if (isRedeliveryExhausted(t)) {
      if (redeliveryExceeded != null) {
        markExceptionAsHandled(t);
        try {
          resultEvent = redeliveryExceeded.process(event);
        } catch (MuleException e) {
          if (e instanceof MessagingException) {
            throw (MessagingException) e;
          }
          throw new MessagingException(event, e);
        }
      } else {
        logger.info("Message redelivery exhausted. No redelivery exhausted actions configured. Message consumed.");
      }
    } else {
      resultEvent = super.route(event, t);
    }
    return resultEvent;
  }

  @Override
  protected Event processReplyTo(Event event, Exception e) {
    if (isRedeliveryExhausted(e)) {
      return super.processReplyTo(event, e);
    } else {
      return event;
    }
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    super.setFlowConstruct(flowConstruct);
    if (redeliveryExceeded != null) {
      redeliveryExceeded.setFlowConstruct(flowConstruct);
    }
  }
}
