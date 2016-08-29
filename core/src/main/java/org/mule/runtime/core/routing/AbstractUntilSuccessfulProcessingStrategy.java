/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.config.i18n.MessageFactory;

import java.io.NotSerializableException;
import java.io.Serializable;

/**
 * Abstract class with common logic for until successful processing strategies.
 */
public abstract class AbstractUntilSuccessfulProcessingStrategy implements UntilSuccessfulProcessingStrategy, MuleContextAware {

  private UntilSuccessfulConfiguration untilSuccessfulConfiguration;
  protected MuleContext muleContext;

  @Override
  public void setUntilSuccessfulConfiguration(final UntilSuccessfulConfiguration untilSuccessfulConfiguration) {
    this.untilSuccessfulConfiguration = untilSuccessfulConfiguration;
  }

  /**
   * Process the event through the configured route in the until-successful configuration.
   *
   * @param event the event to process through the until successful inner route.
   * @return the response from the route if there's no ack expression. If there's ack expression then a message with the response
   *         event but with a payload defined by the ack expression.
   */
  protected MuleEvent processEvent(final MuleEvent event) {
    MuleEvent returnEvent;
    try {
      returnEvent = untilSuccessfulConfiguration.getRoute().process(event);
    } catch (final MuleException me) {
      throw new MuleRuntimeException(me);
    }

    if (returnEvent == null || VoidMuleEvent.getInstance().equals(returnEvent)) {
      return returnEvent;
    }

    final MuleMessage msg = returnEvent.getMessage();
    if (msg == null) {
      throw new MuleRuntimeException(MessageFactory
          .createStaticMessage("No message found in response to processing, which is therefore considered failed for event: "
              + event));
    }

    final boolean errorDetected = untilSuccessfulConfiguration.getFailureExpressionFilter().accept(returnEvent);
    if (errorDetected) {
      throw new MuleRuntimeException(MessageFactory
          .createStaticMessage("Failure expression positive when processing event: " + event));
    }
    return returnEvent;
  }

  /**
   * @param event the response event from the until-successful route.
   * @return the response message to be sent to the until successful caller.
   */
  protected MuleEvent processResponseThroughAckResponseExpression(MuleEvent event) {
    if (event == null || VoidMuleEvent.getInstance().equals(event)) {
      return VoidMuleEvent.getInstance();
    }
    final String ackExpression = getUntilSuccessfulConfiguration().getAckExpression();
    if (ackExpression == null) {
      return event;
    }

    event.setMessage(MuleMessage.builder(event.getMessage())
        .payload(getUntilSuccessfulConfiguration().getMuleContext().getExpressionManager().evaluate(ackExpression, event, null))
        .build());
    return event;
  }

  /**
   * @return configuration of the until-successful router.
   */
  protected UntilSuccessfulConfiguration getUntilSuccessfulConfiguration() {
    return untilSuccessfulConfiguration;
  }

  @Override
  public MuleEvent route(MuleEvent event, FlowConstruct flow) throws MessagingException {
    prepareAndValidateEvent(event);
    return doRoute(event, flow);
  }

  protected abstract MuleEvent doRoute(final MuleEvent event, FlowConstruct flow) throws MessagingException;

  private void prepareAndValidateEvent(final MuleEvent event) throws MessagingException {
    try {
      final MuleMessage message = event.getMessage();
      if (message instanceof MuleMessage) {
        if (message.getDataType().isStreamType()) {
          event.getMessageAsBytes(muleContext);
        } else {
          ensureSerializable(message);
        }
      } else {
        event.getMessageAsBytes(muleContext);
      }
    } catch (final Exception e) {
      throw new MessagingException(MessageFactory.createStaticMessage("Failed to prepare message for processing"), event, e,
                                   getUntilSuccessfulConfiguration().getRouter());
    }
  }

  protected void ensureSerializable(MuleMessage message) throws NotSerializableException {
    if (!(message.getPayload() instanceof Serializable)) {
      throw new NotSerializableException(message.getDataType().getType().getCanonicalName());
    }
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
