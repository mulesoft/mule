/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.security;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.Credentials;
import org.mule.runtime.api.security.CredentialsBuilder;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.api.security.DefaultMuleAuthentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.extension.api.security.AuthenticationHandler;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Default implementation of a {@link AuthenticationHandler}
 *
 * @since 4.0
 */
public class DefaultAuthenticationHandler implements AuthenticationHandler {

  private SecurityContext securityContext;
  private SecurityManager manager;
  private final Consumer<SecurityContext> securityContextUpdater;

  public DefaultAuthenticationHandler(SecurityContext securityContext, SecurityManager manager,
                                      Consumer<SecurityContext> securityContextUpdater) {
    this.securityContext = securityContext;
    this.manager = manager;
    this.securityContextUpdater = securityContextUpdater;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAuthentication(Authentication authentication)
      throws SecurityProviderNotFoundException, SecurityException, UnknownAuthenticationTypeException {

    Authentication authResult = manager.authenticate(authentication);

    if (securityContext == null) {
      securityContext = manager.createSecurityContext(authResult);
    }

    this.securityContext.setAuthentication(authResult);
    this.securityContextUpdater.accept(securityContext);
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
  public Optional<Authentication> getAuthentication() {
    return securityContext != null ? ofNullable(securityContext.getAuthentication()) : empty();
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
