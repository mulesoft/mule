/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.security;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.Credentials;
import org.mule.runtime.api.security.CredentialsBuilder;
import org.mule.runtime.core.api.security.DefaultMuleAuthentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.extension.api.security.AuthenticationHandler;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;

import java.util.List;

/**
 * Default implementation of a {@link AuthenticationHandler}
 *
 * @since 4.0
 */
public class DefaultAuthenticationHandler implements AuthenticationHandler {

  private final MuleSession session;
  private SecurityManager manager;

  public DefaultAuthenticationHandler(SecurityManager manager, MuleSession session) {
    this.session = session;
    this.manager = manager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAuthentication(Authentication authentication)
      throws SecurityProviderNotFoundException, SecurityException, UnknownAuthenticationTypeException {

    Authentication authResult = manager.authenticate(authentication);

    SecurityContext context = manager.createSecurityContext(authResult);
    session.setSecurityContext(context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAuthentication(List<String> securityProviders, Authentication authentication)
      throws SecurityProviderNotFoundException, SecurityException, UnknownAuthenticationTypeException {
    if (!securityProviders.isEmpty()) {
      // This filter may only allow authentication on a subset of registered
      // security providers
      SecurityManager localManager = new DefaultMuleSecurityManager();
      for (String sp : securityProviders) {
        SecurityProvider provider = manager.getProvider(sp);
        if (provider != null) {
          localManager.addProvider(provider);
        } else {
          throw new SecurityProviderNotFoundException(sp);
        }
      }
      this.manager = localManager;
    }

    setAuthentication(authentication);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Authentication getAuthentication() {
    return session.getSecurityContext().getAuthentication();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Authentication createAuthentication(Credentials credentials) {
    return new DefaultMuleAuthentication(credentials);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CredentialsBuilder createCredentialsBuilder() {
    return new DefaultCredentialsBuilder();
  }

}
