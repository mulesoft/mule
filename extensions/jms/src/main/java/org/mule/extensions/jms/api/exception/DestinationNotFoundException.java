/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.exception;

import org.mule.runtime.api.i18n.I18nMessage;

import javax.jms.Destination;

/**
 * Custom Exception thrown when the extension was not able to create a given {@link Destination}
 *
 * @since 4.0
 */
public final class DestinationNotFoundException extends JmsExtensionException {

  public DestinationNotFoundException(I18nMessage message) {
    super(message);
  }

  public DestinationNotFoundException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public DestinationNotFoundException(Throwable cause) {
    super(cause);
  }
}
