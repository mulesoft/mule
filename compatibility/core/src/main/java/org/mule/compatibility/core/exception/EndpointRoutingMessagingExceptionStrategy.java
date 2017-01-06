/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.exception;

import org.mule.compatibility.core.endpoint.outbound.EndpointMulticastingRouter;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;

public class EndpointRoutingMessagingExceptionStrategy extends DefaultMessagingExceptionStrategy {

  @Override
  protected MulticastingRouter buildRouter() {
    // Create an outbound router with all endpoints configured on the exception strategy
    return new EndpointMulticastingRouter();
  }
}
