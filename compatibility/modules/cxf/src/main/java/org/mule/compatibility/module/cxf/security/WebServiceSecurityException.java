/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.security;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.security.SecurityManager;

public class WebServiceSecurityException extends SecurityException {

  public WebServiceSecurityException(Event event, Throwable cause,
                                     SecurityManager securityManager) {
    super(createStaticMessage("Security exception occurred invoking web service\nEndpoint = "
        + event.getContext().getOriginatingConnectorName() + "\nSecurity provider(s) = " + securityManager.getProviders()
        + "\nEvent = " + event), cause);
  }
}


