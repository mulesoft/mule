/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.security.ServerSecurityException;

/**
 * {@code UnsupportedAuthenticationSchemeException} is thrown when a authentication scheme is being used on the message that the
 * Security filter does not understand.
 *
 * @since 4.0
 */
public class UnsupportedAuthenticationSchemeException extends ServerSecurityException implements ErrorMessageAwareException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 3281021140543598681L;

  private Message errorMessage;

  public UnsupportedAuthenticationSchemeException(I18nMessage message, Message errorMessage) {
    super(message);
    this.errorMessage = errorMessage;
  }

  public UnsupportedAuthenticationSchemeException(I18nMessage message, Throwable cause, Message errorMessage) {
    super(message, cause);
    this.errorMessage = errorMessage;
  }

  @Override
  public Message getErrorMessage() {
    return errorMessage;
  }

  @Override
  public Throwable getRootCause() {
    return this;
  }
}
