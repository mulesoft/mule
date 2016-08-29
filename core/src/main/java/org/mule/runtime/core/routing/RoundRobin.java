/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.routing.outbound.AbstractOutboundRouter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * RoundRobin divides the messages it receives among its target routes in round-robin fashion. This includes messages received on
 * all threads, so there is no guarantee that messages received from a splitter are sent to consecutively numbered targets.
 */
public class RoundRobin extends AbstractOutboundRouter {

  /** Index of target route to use */
  AtomicInteger index = new AtomicInteger(1);

  /**
   * Process the event using the next target route in sequence
   */
  @Override
  public MuleEvent route(MuleEvent event) throws MessagingException {
    int modulo = getAndIncrementModuloN(routes.size());
    if (modulo < 0) {
      throw new CouldNotRouteOutboundMessageException(event, this);
    }

    MessageProcessor mp = routes.get(modulo);
    try {
      return doProcessRoute(mp, event);
    } catch (MuleException ex) {
      throw new RoutingException(event, this, ex);
    }
  }

  /**
   * Get the index of the processor to use
   */
  private int getAndIncrementModuloN(int modulus) {
    if (modulus == 0) {
      return -1;
    }
    while (true) {
      int lastIndex = index.get();
      int nextIndex = (lastIndex + 1) % modulus;
      if (index.compareAndSet(lastIndex, nextIndex)) {
        return nextIndex;
      }
    }
  }

  @Override
  public boolean isMatch(MuleEvent message) throws MuleException {
    return true;
  }
}
