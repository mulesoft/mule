/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.config.i18n.CoreMessages.cannotCopyStreamPayload;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.processor.Processor;
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
  protected final Event sendRequest(final Event routedEvent, final InternalMessage message, final Processor route,
                                    boolean awaitResponse)
      throws MuleException {
    Event result;
    try {
      result = sendRequestEvent(routedEvent, message, route, awaitResponse);
    } catch (MuleException me) {
      throw me;
    } catch (Exception e) {
      throw new RoutingException(null, e);
    }

    if (result != null) {
      InternalMessage resultMessage = result.getMessage();
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

  private Event sendRequestEvent(Event routedEvent, InternalMessage message, Processor route, boolean awaitResponse)
      throws MuleException {
    if (route == null) {
      throw new DispatchException(CoreMessages.objectIsNull("route"), null);
    }

    return route.process(createEventToRoute(routedEvent, message, route));
  }

  /**
   * Create a new event to be routed to the target MP
   */
  protected Event createEventToRoute(Event routedEvent, InternalMessage message, Processor route) {
    return Event.builder(routedEvent).message(message).synchronous(true).build();
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
   * @throws MuleException if the payload is consumable
   */
  public static void validateMessageIsNotConsumable(Event event, InternalMessage message) throws MuleException {
    if (message.getPayload().getDataType().isStreamType()) {
      throw new DefaultMuleException(cannotCopyStreamPayload(message.getPayload().getDataType().getType().getName()));
    }
  }

  public static InternalMessage cloneMessage(InternalMessage message) throws MuleException {
    assertNonConsumableMessage(message);
    return message;
  }

  /**
   * Asserts that the {@link Message} in the {@link Event} doesn't carry a consumable payload. This method is useful for routers
   * which need to clone the message before dispatching the message to multiple routes.
   *
   * @throws MuleException If the payload of the message is consumable.
   */
  protected static void assertNonConsumableMessage(InternalMessage message) throws MuleException {
    if (message.getPayload().getDataType().isStreamType()) {
      throw new DefaultMuleException(cannotCopyStreamPayload(message.getPayload().getDataType().getType().getName()));
    }
  }
}
