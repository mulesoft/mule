/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.api.i18n.I18nMessage;

import java.lang.Throwable;

/**
 * Exception thrown when a route for an event can not be found
 */
public class RouteResolverException extends RoutingFailedException {

  public RouteResolverException(Throwable cause) {
    super(cause);
  }

  public RouteResolverException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public RouteResolverException(I18nMessage message) {
    super(message);
  }

}
