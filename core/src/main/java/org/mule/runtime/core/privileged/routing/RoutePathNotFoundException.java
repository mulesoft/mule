/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * <code>RoutePathNotFoundException</code> is thrown if a routing path for an event cannot be found. This can be caused if there
 * is no (or no matching) endpoint for the event to route through.
 */
public class RoutePathNotFoundException extends RoutingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -8481434966594513066L;

  public RoutePathNotFoundException(Processor target) {
    super(target);
  }

  public RoutePathNotFoundException(Processor target, Throwable cause) {
    super(target, cause);
  }

  public RoutePathNotFoundException(I18nMessage message, Processor target) {
    super(message, target);
  }

  public RoutePathNotFoundException(I18nMessage message, Processor target, Throwable cause) {
    super(message, target, cause);
  }
}
