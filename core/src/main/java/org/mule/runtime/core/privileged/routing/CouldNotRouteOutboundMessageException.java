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
 * <code>CouldNotRouteOutboundMessageException</code> thrown if Mule fails to route the current outbound event.
 */

public class CouldNotRouteOutboundMessageException extends RoutingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 4609966704030524483L;

  public CouldNotRouteOutboundMessageException(Processor target) {
    super(target);
  }

  public CouldNotRouteOutboundMessageException(Processor target, Throwable cause) {
    super(target, cause);
  }

  public CouldNotRouteOutboundMessageException(I18nMessage message, Processor target) {
    super(message, target);
  }

  public CouldNotRouteOutboundMessageException(I18nMessage message, Processor target, Throwable cause) {
    super(message, target, cause);
  }
}
