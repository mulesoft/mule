/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.routing.RoutingException;
import org.mule.runtime.core.privileged.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.privileged.routing.outbound.AbstractOutboundRouter;

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
  public CoreEvent route(CoreEvent event) throws MuleException {
    int modulo = getAndIncrementModuloN(routes.size());
    if (modulo < 0) {
      throw new CouldNotRouteOutboundMessageException(this);
    }

    Processor mp = routes.get(modulo);
    try {
      return doProcessRoute(mp, event);
    } catch (MuleException ex) {
      throw new RoutingException(this, ex);
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
  public boolean isMatch(CoreEvent message, CoreEvent.Builder builder) throws MuleException {
    return true;
  }
}
