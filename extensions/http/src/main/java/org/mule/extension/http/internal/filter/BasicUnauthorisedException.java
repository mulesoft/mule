/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.filter;

import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.security.SecurityFilter;

public class BasicUnauthorisedException extends UnauthorisedException implements ErrorMessageAwareException {

  private Message errorMessage;

  public BasicUnauthorisedException(I18nMessage message, Message errorMessage) {
    super(message);
    this.errorMessage = errorMessage;
  }

  public BasicUnauthorisedException(I18nMessage message, Throwable cause, Message errorMessage) {
    super(message, cause);
    this.errorMessage = errorMessage;
  }

  public BasicUnauthorisedException(Event event, SecurityContext context, SecurityFilter filter) {
    super(context, filter.getClass().getName(), event.getContext().getOriginatingConnectorName());
    this.errorMessage = event.getMessage();
  }

  public BasicUnauthorisedException(SecurityContext context, String filter, String connector, Message errorMessage) {
    super(context, filter, connector);
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
