/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.security;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import org.mule.runtime.api.artifact.ServiceDiscoverer;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.runtime.core.api.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * <code>AbstractSecurityFilter</code> provides basic initialisation for all security filters, namely configuring the
 * SecurityManager for this instance
 */
public abstract class AbstractSecurityFilter implements SecurityFilter {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  protected SecurityManager securityManager;
  @Inject
  protected ServiceDiscoverer serviceDiscoverer;

  private String securityProviders;

  @Override
  public final void initialise() throws InitialisationException {
    if (securityManager == null) {
      securityManager = (SecurityManager) serviceDiscoverer.lookupByName(OBJECT_SECURITY_MANAGER).get();
    }

    if (securityManager == null) {
      throw new InitialisationException(CoreMessages.authSecurityManagerNotSet(), this);
    }

    // This filter may only allow authentication on a subset of registered
    // security providers
    if (securityProviders != null) {
      SecurityManager localManager = new DefaultMuleSecurityManager();
      String[] securityProviders = StringUtils.splitAndTrim(this.securityProviders, ",");
      for (String sp : securityProviders) {
        SecurityProvider provider = securityManager.getProvider(sp);
        if (provider != null) {
          localManager.addProvider(provider);
        } else {
          throw new InitialisationException(CoreMessages.objectNotRegistered("Security Provider", sp), this);
        }
      }
      securityManager = localManager;
    }

    doInitialise();
  }

  protected void doInitialise() throws InitialisationException {}

  /** @param manager */
  @Override
  public void setSecurityManager(SecurityManager manager) {
    securityManager = manager;
  }

  @Override
  public SecurityManager getSecurityManager() {
    return securityManager;
  }

  @Override
  public String getSecurityProviders() {
    return securityProviders;
  }

  @Override
  public void setSecurityProviders(String providers) {
    securityProviders = providers;
  }

  @Override
  public abstract Event doFilter(Event event) throws SecurityException, UnknownAuthenticationTypeException,
      CryptoFailureException, SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException;

}
