/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.security;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.AuthenticationFilter;
import org.mule.runtime.core.api.security.CredentialsAccessor;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnknownAuthenticationTypeException;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides a framework to perform inbound or outbound authentication for messages.
 */
public abstract class AbstractAuthenticationFilter extends AbstractSecurityFilter implements AuthenticationFilter {

  private boolean authenticate;
  private CredentialsAccessor credentialsAccessor;

  public CredentialsAccessor getCredentialsAccessor() {
    return credentialsAccessor;
  }

  public void setCredentialsAccessor(CredentialsAccessor credentialsAccessor) {
    this.credentialsAccessor = credentialsAccessor;
  }

  public boolean isAuthenticate() {
    return authenticate;
  }

  public void setAuthenticate(boolean authenticate) {
    this.authenticate = authenticate;
  }

  @Override
  public void doFilter(MuleEvent event) throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
    authenticate(event);
  }

  public abstract void authenticate(MuleEvent event) throws SecurityException, UnknownAuthenticationTypeException,
      CryptoFailureException, SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException;

}
