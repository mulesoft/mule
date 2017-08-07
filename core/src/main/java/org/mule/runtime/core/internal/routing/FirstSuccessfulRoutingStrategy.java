/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.DefaultEventContext.child;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotCopyStreamPayload;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.util.StringMessageUtils.truncate;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.DefaultTransformationService;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutingException;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Routing strategy that routes the message through a list of {@link Processor} until one is successfully executed.
 *
 * The message will be route to the first route, if the route execution is successful then execution ends, if not the message will
 * be route to the next route. This continues until a successful route is found.
 */
public class FirstSuccessfulRoutingStrategy implements RoutingStrategy {

  @Inject
  private DefaultTransformationService transformationService;

  /**
   * logger used by this class
   */
  protected static transient Logger logger = LoggerFactory.getLogger(FirstSuccessfulRoutingStrategy.class);
  private final ExpressionManager expressionManager;
  private final ComponentLocation componentLocation;
  private final String failureExpression;
  private final RouteProcessor processor;

  /**
   * @param failureExpression Mule expression that validates if a {@link Processor} execution was successful or not.
   */
  public FirstSuccessfulRoutingStrategy(ExpressionManager expressionManager,
                                        final String failureExpression,
                                        RouteProcessor processor,
                                        ComponentLocation componentLocation) {
    this.expressionManager = expressionManager;
    this.componentLocation = componentLocation;
    this.failureExpression = failureExpression;
    this.processor = processor;
  }

  /**
   * Validates that the payload is not consumable so it can be copied.
   *
   * If validation fails then throws a MessagingException
   *
   * @param message
   * @throws MuleException if the payload is consumable
   */
  public static void validateMessageIsNotConsumable(Message message) throws MuleException {
    if (message.getPayload().getDataType().isStreamType()) {
      throw new DefaultMuleException(cannotCopyStreamPayload(message.getPayload().getDataType().getType().getName()));
    }
  }

  @Override
  public Event route(Event event, List<Processor> messageProcessors) throws MuleException {
    Event returnEvent = null;

    boolean failed = true;
    Exception failExceptionCause = null;

    validateMessageIsNotConsumable(event.getMessage());

    for (Processor mp : messageProcessors) {
      try {
        returnEvent = processor.processRoute(mp, builder(child(event.getInternalContext(), empty()), event).build());

        if (returnEvent == null) {
          failed = false;
        } else if (returnEvent.getMessage() == null) {
          failed = true;
        } else {
          failed = expressionManager.evaluateBoolean(failureExpression, returnEvent,
                                                     fromSingleComponent(componentLocation.getRootContainerName()), false, true);
        }
      } catch (Exception ex) {
        failed = true;
        failExceptionCause = ex;
      }
      if (!failed) {
        break;
      }
    }

    if (failed) {
      if (failExceptionCause != null) {
        throw new RoutingFailedException(createStaticMessage("All processors failed during 'first-successful' routing strategy"),
                                         failExceptionCause);
      } else {
        throw new RoutingFailedException(createStaticMessage("All processors failed during 'first-successful' routing strategy"));
      }
    }

    return returnEvent != null ? builder(event.getInternalContext(), returnEvent).build() : null;
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
  protected final Event sendRequest(final Event routedEvent, final Message message, final Processor route,
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
      Message resultMessage = result.getMessage();
      if (logger.isTraceEnabled()) {
        if (resultMessage != null) {
          try {
            logger.trace("Response payload: \n"
                + truncate(transformationService.getPayloadForLogging(resultMessage), 100, false));
          } catch (Exception e) {
            logger.trace("Response payload: \n(unable to retrieve payload: " + e.getMessage());
          }
        }
      }
    }
    return result;
  }

  private Event sendRequestEvent(Event routedEvent, Message message, Processor route, boolean awaitResponse)
      throws MuleException {
    if (route == null) {
      throw new DispatchException(objectIsNull("route"), null);
    }

    return route.process(routedEvent);
  }

  interface RouteProcessor {

    Event processRoute(Processor route, Event event) throws MuleException;
  }
}
