/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.config.i18n.I18nMessageFactory;
import org.mule.runtime.core.exception.MessagingException;

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
  protected Event processEvent(final Event event) {
    Event returnEvent;
    try {
      returnEvent = untilSuccessfulConfiguration.getRoute().process(event);
    } catch (final MuleException me) {
      throw new MuleRuntimeException(me);
    }

    if (returnEvent == null) {
      return returnEvent;
    }

    final InternalMessage msg = returnEvent.getMessage();
    if (msg == null) {
      throw new MuleRuntimeException(I18nMessageFactory
          .createStaticMessage("No message found in response to processing, which is therefore considered failed for event: "
              + event));
    }

    Builder builder = Event.builder(returnEvent);
    final boolean errorDetected = untilSuccessfulConfiguration.getFailureExpressionFilter().accept(returnEvent, builder);
    if (errorDetected) {
      throw new MuleRuntimeException(I18nMessageFactory
          .createStaticMessage("Failure expression positive when processing event: " + event));
    }
    return builder.build();
  }

  /**
   * @param event the response event from the until-successful route.
   * @return the response message to be sent to the until successful caller.
   */
  protected Event processResponseThroughAckResponseExpression(Event event) {
    if (event == null) {
      return null;
    }
    final String ackExpression = getUntilSuccessfulConfiguration().getAckExpression();
    if (ackExpression == null) {
      return event;
    }

    return Event.builder(event).message(InternalMessage.builder(event.getMessage())
        .payload(getUntilSuccessfulConfiguration().getMuleContext().getExpressionLanguage().evaluate(ackExpression, event, null))
        .build()).build();
  }

  /**
   * @return configuration of the until-successful router.
   */
  protected UntilSuccessfulConfiguration getUntilSuccessfulConfiguration() {
    return untilSuccessfulConfiguration;
  }

  @Override
  public Event route(Event event, FlowConstruct flow) throws MuleException {
    prepareAndValidateEvent(event);
    return doRoute(event, flow);
  }

  protected abstract Event doRoute(final Event event, FlowConstruct flow) throws MuleException;

  private void prepareAndValidateEvent(final Event event) throws MessagingException {
    try {
      final InternalMessage message = event.getMessage();
      if (message instanceof InternalMessage) {
        if (message.getPayload().getDataType().isStreamType()) {
          event.getMessageAsBytes(muleContext);
        } else {
          ensureSerializable(message);
        }
      } else {
        event.getMessageAsBytes(muleContext);
      }
    } catch (final Exception e) {
      throw new MessagingException(I18nMessageFactory.createStaticMessage("Failed to prepare message for processing"), event, e,
                                   getUntilSuccessfulConfiguration().getRouter());
    }
  }

  protected void ensureSerializable(InternalMessage message) throws NotSerializableException {
    if (!(message.getPayload().getValue() instanceof Serializable)) {
      throw new NotSerializableException(message.getPayload().getDataType().getType().getCanonicalName());
    }
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
