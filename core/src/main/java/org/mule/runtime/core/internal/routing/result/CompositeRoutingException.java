package org.mule.runtime.core.internal.routing.result;

import org.mule.runtime.core.privileged.routing.RoutingResult;

public class CompositeRoutingException extends org.mule.runtime.core.privileged.routing.CompositeRoutingException {

  private static final long serialVersionUID = -4421728527040579605L;

  /**
   * Constructs a new {@link CompositeRoutingException}
   *
   * @param routingResult routing result object containing the results from all routes.
   */
  public CompositeRoutingException(RoutingResult routingResult) {
    super(routingResult);
  }
}
