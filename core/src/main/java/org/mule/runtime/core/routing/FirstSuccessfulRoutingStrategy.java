/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.DefaultEventContext.child;
import static org.mule.runtime.core.api.Event.builder;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

/**
 *
 * Routing strategy that routes the message through a list of {@link Processor} until one is successfully executed.
 *
 * The message will be route to the first route, if the route execution is successful then execution ends, if not the message will
 * be route to the next route. This continues until a successful route is found.
 */
public class FirstSuccessfulRoutingStrategy extends AbstractRoutingStrategy {

  private FlowConstruct flowConstruct;
  private String failureExpression;
  private RouteProcessor processor;

  /**
   * @param flowConstruct
   * @param failureExpression Mule expression that validates if a {@link Processor} execution was successful or not.
   */
  public FirstSuccessfulRoutingStrategy(final FlowConstruct flowConstruct, final String failureExpression,
                                        RouteProcessor processor) {
    super(flowConstruct.getMuleContext());
    this.flowConstruct = flowConstruct;
    this.failureExpression = failureExpression;
    this.processor = processor;
  }

  @Override
  public Event route(Event event, List<Processor> messageProcessors) throws MuleException {
    Event returnEvent = null;

    boolean failed = true;
    Exception failExceptionCause = null;

    validateMessageIsNotConsumable(event.getMessage());

    for (Processor mp : messageProcessors) {
      try {
        returnEvent = processor.processRoute(mp, builder(child(event.getContext(), empty()), event).build());

        if (returnEvent == null) {
          failed = false;
        } else if (returnEvent.getMessage() == null) {
          failed = true;
        } else {
          failed = getMuleContext().getExpressionManager()
              .evaluateBoolean(failureExpression, returnEvent, flowConstruct, false, true);
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
        throw new RoutingFailedException(createStaticMessage("all message processor failed during first successful routing strategy"),
                                         failExceptionCause);
      } else {
        throw new RoutingFailedException(createStaticMessage("all message processor failed during first successful routing strategy"));
      }
    }

    return returnEvent != null ? builder(event.getContext(), returnEvent).build() : null;
  }

  interface RouteProcessor {

    Event processRoute(Processor route, Event event) throws MuleException;
  }
}
