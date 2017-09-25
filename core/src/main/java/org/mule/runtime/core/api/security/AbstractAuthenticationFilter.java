/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides a framework to perform inbound or outbound authentication for messages.
 */
public abstract class AbstractAuthenticationFilter extends AbstractSecurityFilter implements AuthenticationFilter {

  private boolean authenticate;

  public boolean isAuthenticate() {
    return authenticate;
  }

  @Override
  public SecurityContext doFilter(CoreEvent event)
      throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
    return authenticate(event);
  }

  @Override
  public abstract SecurityContext authenticate(CoreEvent event) throws SecurityException, UnknownAuthenticationTypeException,
      CryptoFailureException, SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException;

}
