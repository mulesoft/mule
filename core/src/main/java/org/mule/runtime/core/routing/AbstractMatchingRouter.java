/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.util.ClassUtils.isConsumable;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.MatchableMessageProcessor;
import org.mule.runtime.core.api.routing.MatchingRouter;
import org.mule.runtime.core.api.routing.TransformingMatchable;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractRouterCollection</code> provides common method implementations of router collections for in and outbound routers.
 */

public class AbstractMatchingRouter extends AbstractAnnotatedObject implements MatchingRouter {

  /**
   * logger used by this class
   */
  protected final transient Logger logger = LoggerFactory.getLogger(getClass());

  protected List<MatchableMessageProcessor> matchableRoutes = new CopyOnWriteArrayList<MatchableMessageProcessor>();
  protected boolean matchAll = false;
  protected MessageProcessor defaultRoute;

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    MuleMessage message = event.getMessage();
    MuleEvent result;
    boolean matchfound = false;

    for (Iterator<MatchableMessageProcessor> iterator = matchableRoutes.iterator(); iterator.hasNext();) {
      MatchableMessageProcessor outboundRouter = iterator.next();

      final MuleEvent eventToRoute;

      boolean copyEvent = false;
      // Create copy of message for router 1..n-1 if matchAll="true" or if
      // routers require copy because it may mutate payload before match is
      // chosen
      if (iterator.hasNext()) {
        if (isMatchAll()) {
          copyEvent = true;
        } else if (outboundRouter instanceof TransformingMatchable) {
          copyEvent = ((TransformingMatchable) outboundRouter).isTransformBeforeMatch();
        }
      }

      if (copyEvent) {
        if (isConsumable(message.getDataType().getType())) {
          throw new MessagingException(CoreMessages.cannotCopyStreamPayload(message.getDataType().getType().getName()), event,
                                       this);
        }
        eventToRoute = OptimizedRequestContext.criticalSetEvent(event);
      } else {
        eventToRoute = event;
      }

      if (outboundRouter.isMatch(eventToRoute)) {
        matchfound = true;
        result = outboundRouter.process(event);
        if (!isMatchAll()) {
          return result;
        }
      }
    }

    if (!matchfound && defaultRoute != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Message did not match any routers on: " + event.getFlowConstruct().getName()
            + " invoking catch all strategy");
      }
      return processDefaultRoute(event);
    } else if (!matchfound) {
      logger.warn("Message did not match any routers on: " + event.getFlowConstruct().getName()
          + " and there is no catch all strategy configured on this router.  Disposing message " + message);
    }
    return event;
  }

  protected MuleEvent processDefaultRoute(MuleEvent event) throws MuleException {
    return defaultRoute.process(event);
  }

  public boolean isMatchAll() {
    return matchAll;
  }

  public void setMatchAll(boolean matchAll) {
    this.matchAll = matchAll;
  }

  @Override
  public void addRoute(MatchableMessageProcessor matchable) {
    matchableRoutes.add(matchable);
  }

  @Override
  public void removeRoute(MatchableMessageProcessor matchable) {
    matchableRoutes.remove(matchable);
  }

  public void setDefaultRoute(MessageProcessor defaultRoute) {
    this.defaultRoute = defaultRoute;
  }

  public List<MatchableMessageProcessor> getRoutes() {
    return matchableRoutes;
  }

  public MessageProcessor getDefaultRoute() {
    return defaultRoute;
  }

  public void initialise() throws InitialisationException {
    for (MatchableMessageProcessor route : matchableRoutes) {
      if (route instanceof Initialisable) {
        ((Initialisable) route).initialise();
      }
    }
  }

  public void dispose() {
    for (MatchableMessageProcessor route : matchableRoutes) {
      if (route instanceof Disposable) {
        ((Disposable) route).dispose();
      }
    }
  }
}
