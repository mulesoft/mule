/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.privileged.routing.outbound.AbstractOutboundRouter;

/**
 * FirstSuccessful routes an event to the first target route that can accept it without throwing or returning an exception. If no
 * such route can be found, an exception is thrown. Note that this works more reliable with synchronous targets, but no such
 * restriction is imposed.
 */
public class FirstSuccessful extends AbstractOutboundRouter {

  private RoutingStrategy routingStrategy;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    routingStrategy =
        new FirstSuccessfulRoutingStrategy(this::doProcessRoute);
  }

  /**
   * Route the given event to one of our targets
   */
  @Override
  public CoreEvent route(CoreEvent event) throws MuleException {
    try {
      return routingStrategy.route(event, getRoutes());
    } catch (RoutingFailedException e) {
      throw new CouldNotRouteOutboundMessageException(this, e);
    }
  }

  @Override
  public boolean isMatch(CoreEvent event, CoreEvent.Builder builder) throws MuleException {
    return true;
  }
}
