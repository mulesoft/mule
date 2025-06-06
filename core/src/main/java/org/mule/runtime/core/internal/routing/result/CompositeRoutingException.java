/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.result;

import org.mule.runtime.core.privileged.routing.RoutingResult;

/**
 * This is kept for backwards compatibility with persistent serialized data.
 *
 * @since 3.5.0
 */
public final class CompositeRoutingException extends org.mule.runtime.core.privileged.routing.CompositeRoutingException {

  private static final long serialVersionUID = -4421728527040579605L;

  /**
   * Constructs a new {@link org.mule.runtime.core.privileged.routing.CompositeRoutingException}
   *
   * @param routingResult routing result object containing the results from all routes.
   */
  public CompositeRoutingException(RoutingResult routingResult) {
    super(routingResult);
  }
}
