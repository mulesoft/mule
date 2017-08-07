/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.authFailedForUser;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authSetButNoContext;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authorizationDeniedOnEndpoint;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.ServerSecurityException;
import org.mule.runtime.core.api.Event;

/**
 * <code>NotPermittedException</code> is thrown if the user isn't authorized to perform an action.
 */
public class NotPermittedException extends ServerSecurityException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -6664384216189042673L;

  public NotPermittedException(I18nMessage message) {
    super(message);
  }

  public NotPermittedException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public NotPermittedException(Event event, SecurityContext context, SecurityFilter filter) {
    super(constructMessage(context,
                           event.getContext().getOriginatingLocation().getComponentIdentifier().getIdentifier().getNamespace(),
                           filter));
  }

  private static I18nMessage constructMessage(SecurityContext context,
                                              String originatingConnectorName,
                                              SecurityFilter filter) {
    I18nMessage m;
    if (context == null) {
      m = authSetButNoContext(filter.getClass().getName());
    } else {
      m = authFailedForUser(context.getAuthentication().getPrincipal());
    }
    m.setNextMessage(authorizationDeniedOnEndpoint(originatingConnectorName));
    return m;
  }
}
