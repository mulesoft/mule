/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security.filter;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.authFailedForUser;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.encryptionStrategyNotSet;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.Credentials;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.security.CredentialsAccessor;
import org.mule.runtime.core.internal.security.CredentialsNotSetException;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.api.security.DefaultMuleAuthentication;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;
import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.UnauthorisedException;
import org.mule.runtime.core.internal.security.MuleHeaderCredentialsAccessor;

/**
 * <code>MuleEncryptionEndpointSecurityFilter</code> provides password-based encryption
 */
public class MuleEncryptionEndpointSecurityFilter extends AbstractOperationSecurityFilter {

  private EncryptionStrategy strategy;
  private final CredentialsAccessor credentialsAccessor;

  public MuleEncryptionEndpointSecurityFilter(EncryptionStrategy strategy) {
    this.strategy = strategy;
    this.credentialsAccessor = new MuleHeaderCredentialsAccessor();
  }

  @Override
  protected SecurityContext authenticateInbound(CoreEvent event) throws SecurityException, SecurityProviderNotFoundException,
      CryptoFailureException, EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException {
    String userHeader = (String) credentialsAccessor.getCredentials(event);
    if (userHeader == null) {
      throw new CredentialsNotSetException(event, event.getSecurityContext(), this);
    }

    Credentials user = new DefaultMuleCredentials(userHeader, getSecurityManager());

    Authentication authentication;
    try {
      authentication = getSecurityManager().authenticate(new DefaultMuleAuthentication(user));
    } catch (Exception e) {
      // Authentication failed
      if (logger.isDebugEnabled()) {
        logger.debug("Authentication request for user: " + user.getUsername() + " failed: " + e.toString());
      }
      throw new UnauthorisedException(authFailedForUser(user.getUsername()), e);
    }

    // Authentication success
    if (logger.isDebugEnabled()) {
      logger.debug("Authentication success: " + authentication.toString());
    }

    SecurityContext context = getSecurityManager().createSecurityContext(authentication);
    context.setAuthentication(authentication);
    return context;
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    if (strategy == null) {
      throw new InitialisationException(encryptionStrategyNotSet(), this);
    }
  }

}
