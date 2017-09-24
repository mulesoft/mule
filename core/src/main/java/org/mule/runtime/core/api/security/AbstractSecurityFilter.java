/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authSecurityManagerNotSet;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectNotRegistered;
import static org.mule.runtime.core.api.util.StringUtils.splitAndTrim;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractSecurityFilter</code> provides basic initialisation for all security filters, namely configuring the
 * SecurityManager for this instance
 */
public abstract class AbstractSecurityFilter extends AbstractComponent implements SecurityFilter {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  protected SecurityManager securityManager;
  @Inject
  protected Registry registry;

  private String securityProviders;

  @Override
  public final void initialise() throws InitialisationException {
    if (securityManager == null) {
      securityManager = (SecurityManager) registry.lookupByName(OBJECT_SECURITY_MANAGER).get();
    }

    if (securityManager == null) {
      throw new InitialisationException(authSecurityManagerNotSet(), this);
    }

    // This filter may only allow authentication on a subset of registered
    // security providers
    if (securityProviders != null) {
      SecurityManager localManager = new DefaultMuleSecurityManager();
      String[] securityProviders = splitAndTrim(this.securityProviders, ",");
      for (String sp : securityProviders) {
        SecurityProvider provider = securityManager.getProvider(sp);
        if (provider != null) {
          localManager.addProvider(provider);
        } else {
          throw new InitialisationException(objectNotRegistered("Security Provider", sp), this);
        }
      }
      securityManager = localManager;
    }

    doInitialise();
  }

  protected void doInitialise() throws InitialisationException {}

  @Override
  public SecurityManager getSecurityManager() {
    return securityManager;
  }

  @Override
  public String getSecurityProviders() {
    return securityProviders;
  }

  @Override
  public abstract SecurityContext doFilter(CoreEvent event) throws SecurityException, UnknownAuthenticationTypeException,
      CryptoFailureException, SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException;

}
