/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.filters;

import static org.mule.compatibility.transport.http.HttpConnector.HTTP_PARAMS_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_AUTHORIZATION;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.security.Authentication;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <code>HttpBasicAuthenticationFilter</code> TODO
 */
public class HttpBasicAuthenticationFilter extends org.mule.runtime.module.http.internal.filter.HttpBasicAuthenticationFilter {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(HttpBasicAuthenticationFilter.class);

  public HttpBasicAuthenticationFilter(String realm) {
    this.setRealm(realm);
  }

  /**
   * Authenticates the current message if authenticate is set to true. This method will always populate the secure context in the
   * session
   *
   * @param event the current event being dispatched
   * @throws org.mule.api.security.SecurityException if authentication fails
   */
  public void authenticateOutbound(MuleEvent event) throws SecurityException, SecurityProviderNotFoundException {
    SecurityContext securityContext = event.getSession().getSecurityContext();
    if (securityContext == null) {
      if (isAuthenticate()) {
        throw new UnauthorisedException(event, securityContext, this);
      } else {
        return;
      }
    }

    Authentication auth = securityContext.getAuthentication();
    if (isAuthenticate()) {
      auth = getSecurityManager().authenticate(auth);
      if (logger.isDebugEnabled()) {
        logger.debug("Authentication success: " + auth.toString());
      }
    }

    StringBuilder header = new StringBuilder(128);
    header.append("Basic ");
    String token = auth.getCredentials().toString();
    header.append(new String(Base64.encodeBase64(token.getBytes())));

    event
        .setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty(HEADER_AUTHORIZATION, header.toString()).build());
  }
}
