/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Exception through by routing strategies when routing fails
 */
public class RoutingFailedException extends MuleException {

  public RoutingFailedException(I18nMessage message) {
    super(message);
  }

  public RoutingFailedException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public RoutingFailedException(Throwable cause) {
    super(cause);
  }

}
