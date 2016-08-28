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
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.StringMessageUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract routing strategy with utility methods to be reused by routing strategies
 */
public abstract class AbstractRoutingStrategy implements RoutingStrategy {

  /**
   * logger used by this class
   */
  protected static transient Logger logger = LoggerFactory.getLogger(AbstractRoutingStrategy.class);

  private final MuleContext muleContext;

  public AbstractRoutingStrategy(final MuleContext muleContext) {
    this.muleContext = muleContext;
  }


  /**
   * Send message event to destination.
   *
   * Creates a new event that will be used to process the route.
   *
   * @param routedEvent event to route
   * @param message message to route
   * @param route message processor to be executed
   * @param awaitResponse if the
   * @return
   * @throws MuleException
   */
  protected final MuleEvent sendRequest(final MuleEvent routedEvent, final MuleMessage message, final MessageProcessor route,
                                        boolean awaitResponse)
      throws MuleException {
    MuleEvent result;
    try {
      result = sendRequestEvent(routedEvent, message, route, awaitResponse);
    } catch (MessagingException me) {
      throw me;
    } catch (Exception e) {
      throw new RoutingException(routedEvent, null, e);
    }

    if (result != null && !VoidMuleEvent.getInstance().equals(result)) {
      MuleMessage resultMessage = result.getMessage();
      if (logger.isTraceEnabled()) {
        if (resultMessage != null) {
          try {
            logger.trace("Response payload: \n" + StringMessageUtils
                .truncate(muleContext.getTransformationService().getPayloadForLogging(resultMessage), 100, false));
          } catch (Exception e) {
            logger.trace("Response payload: \n(unable to retrieve payload: " + e.getMessage());
          }
        }
      }
    }
    return result;
  }

  private MuleEvent sendRequestEvent(MuleEvent routedEvent, MuleMessage message, MessageProcessor route, boolean awaitResponse)
      throws MuleException {
    if (route == null) {
      throw new DispatchException(CoreMessages.objectIsNull("route"), routedEvent, null);
    }

    return route.process(createEventToRoute(routedEvent, message, route));
  }

  /**
   * Create a new event to be routed to the target MP
   */
  protected MuleEvent createEventToRoute(MuleEvent routedEvent, MuleMessage message, MessageProcessor route) {
    return MuleEvent.builder(routedEvent).message(message).synchronous(true).build();
  }

  protected MuleContext getMuleContext() {
    return muleContext;
  }

  /**
   * Validates that the payload is not consumable so it can be copied.
   *
   * If validation fails then throws a MessagingException
   *
   * @param event
   * @param message
   * @throws MessagingException
   */
  public static void validateMessageIsNotConsumable(MuleEvent event, MuleMessage message) throws MessagingException {
    if (message.getDataType().isStreamType()) {
      throw new MessagingException(CoreMessages.cannotCopyStreamPayload(message.getDataType().getType().getName()), event);
    }
  }

  public static MuleMessage cloneMessage(MuleEvent event, MuleMessage message) throws MessagingException {
    assertNonConsumableMessage(event, message);
    return message;
  }

  /**
   * Asserts that the {@link MuleMessage} in the {@link MuleEvent} doesn't carry a consumable payload. This method is useful for
   * routers which need to clone the message before dispatching the message to multiple routes.
   *
   * @param event The {@link MuleEvent}.
   * @param event The {@link MuleMessage} whose payload is to be verified.
   * @throws MessagingException If the payload of the message is consumable.
   */
  protected static void assertNonConsumableMessage(MuleEvent event, MuleMessage message) throws MessagingException {
    if (message.getDataType().isStreamType()) {
      throw new MessagingException(CoreMessages.cannotCopyStreamPayload(message.getDataType().getType().getName()), event);
    }
  }
}
