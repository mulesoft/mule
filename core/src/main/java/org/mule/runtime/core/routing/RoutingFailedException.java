/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.config.i18n.Message;

/**
 * Exception through by routing strategies when routing fails
 */
public class RoutingFailedException extends MuleException {

  public RoutingFailedException(Message message) {
    super(message);
  }

  public RoutingFailedException(Message message, Throwable cause) {
    super(message, cause);
  }

  public RoutingFailedException(Throwable cause) {
    super(cause);
  }

}
