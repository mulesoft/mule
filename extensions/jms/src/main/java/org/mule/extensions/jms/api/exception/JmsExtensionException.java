/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.exception;

import org.mule.runtime.api.exception.ExceptionHelper;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Custom generic exception for JmsExtension thrown errors
 *
 * @since 4.0
 */
public class JmsExtensionException extends MuleException {

  /**
   * {@inheritDoc}
   */
  public JmsExtensionException(I18nMessage message) {
    super();
    setMessage(message);
  }

  /**
   * {@inheritDoc}
   */
  public JmsExtensionException(I18nMessage message, Throwable cause) {
    super(ExceptionHelper.unwrap(cause));
    setMessage(message);
  }

  /**
   * {@inheritDoc}
   */
  public JmsExtensionException(Throwable cause) {
    super(cause);
  }

}
