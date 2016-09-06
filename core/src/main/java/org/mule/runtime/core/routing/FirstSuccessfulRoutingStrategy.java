/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.config.i18n.CoreMessages.createStaticMessage;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEvent.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.routing.filters.ExpressionFilter;

import java.util.List;

/**
 *
 * Routing strategy that routes the message through a list of {@link MessageProcessor} until one is successfully executed.
 *
 * The message will be route to the first route, if the route execution is successful then execution ends, if not the message will
 * be route to the next route. This continues until a successful route is found.
 */
public class FirstSuccessfulRoutingStrategy extends AbstractRoutingStrategy {

  protected ExpressionFilter failureExpressionFilter;
  private RouteProcessor processor;

  /**
   * @param muleContext
   * @param failureExpression Mule expression that validates if a {@link MessageProcessor} execution was successful or not.
   */
  public FirstSuccessfulRoutingStrategy(final MuleContext muleContext, final String failureExpression, RouteProcessor processor) {
    super(muleContext);
    if (failureExpression != null) {
      failureExpressionFilter = new ExpressionFilter(failureExpression);
    } else {
      failureExpressionFilter = new ExpressionFilter("exception != null");
    }
    failureExpressionFilter.setMuleContext(muleContext);
    this.processor = processor;
  }

  @Override
  public MuleEvent route(MuleEvent event, List<MessageProcessor> messageProcessors) throws MuleException {
    MuleEvent returnEvent = null;

    boolean failed = true;
    Exception failExceptionCause = null;
    for (MessageProcessor mp : messageProcessors) {
      try {
        MuleEvent toProcess = cloneEventForRouting(event, mp);
        returnEvent = processor.processRoute(mp, toProcess);

        if (returnEvent == null || VoidMuleEvent.getInstance().equals(returnEvent)) {
          failed = false;
        } else if (returnEvent.getMessage() == null) {
          failed = true;
        } else {
          Builder builder = MuleEvent.builder(returnEvent);
          failed = returnEvent == null || failureExpressionFilter.accept(returnEvent, builder);
          returnEvent = builder.build();
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

    return returnEvent;
  }

  private MuleEvent cloneEventForRouting(MuleEvent event, MessageProcessor mp) throws MuleException {
    return createEventToRoute(event, cloneMessage(event.getMessage()), mp);
  }

  interface RouteProcessor {

    MuleEvent processRoute(MessageProcessor route, MuleEvent event) throws MuleException;
  }
}
