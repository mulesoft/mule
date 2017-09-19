/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.authorizationAttemptFailed;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.internal.lifecycle.LifecycleTransitionResult;
import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.runtime.api.security.UnauthorisedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code DefaultMuleSecurityManager} is a default implementation of a {@link SecurityManager} for a Mule instance.
 *
 * @since 4.0
 */
public class DefaultMuleSecurityManager extends AbstractComponent implements SecurityManager {

  /**
   * logger used by this class
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMuleSecurityManager.class);

  private Map<String, SecurityProvider> providers = new ConcurrentHashMap<>();
  private Map<String, EncryptionStrategy> cryptoStrategies = new ConcurrentHashMap<>();

  public DefaultMuleSecurityManager() {
    super();
  }

  @Override
  public void initialise() throws InitialisationException {
    List<Initialisable> all = new LinkedList<>(providers.values());
    // ordering: appends
    all.addAll(cryptoStrategies.values());
    LifecycleTransitionResult.initialiseAll(all.iterator());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Authentication authenticate(Authentication authentication) throws SecurityException, SecurityProviderNotFoundException {
    Iterator<SecurityProvider> iter = providers.values().iterator();
    Class<? extends Authentication> toTest = authentication.getClass();

    while (iter.hasNext()) {
      SecurityProvider provider = iter.next();

      if (provider.supports(toTest)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Authentication attempt using " + provider.getClass().getName());
        }

        Authentication result = null;
        try {
          result = provider.authenticate(authentication);
        } catch (Exception e) {
          if (!iter.hasNext()) {
            throw new UnauthorisedException(authorizationAttemptFailed(), e);
          }
        }

        if (result != null) {
          return result;
        }
      }
    }

    throw new SecurityProviderNotFoundException(toTest.getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addProvider(SecurityProvider provider) {
    if (getProvider(provider.getName()) != null) {
      throw new IllegalArgumentException("Provider already registered: " + provider.getName());
    }
    providers.put(provider.getName(), provider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SecurityProvider getProvider(String name) {
    if (name == null) {
      throw new IllegalArgumentException("provider Name cannot be null");
    }
    return providers.get(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SecurityProvider removeProvider(String name) {
    return providers.remove(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<SecurityProvider> getProviders() {
    ArrayList<SecurityProvider> providersList = new ArrayList<>(providers.values());
    return Collections.unmodifiableCollection(providersList);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProviders(Collection<SecurityProvider> providers) {
    for (SecurityProvider provider : providers) {
      addProvider(provider);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SecurityContext createSecurityContext(Authentication authentication) throws UnknownAuthenticationTypeException {
    Iterator<SecurityProvider> iter = providers.values().iterator();
    Class<? extends Authentication> toTest = authentication.getClass();

    while (iter.hasNext()) {
      SecurityProvider provider = iter.next();
      if (provider.supports(toTest)) {
        return provider.createSecurityContext(authentication);
      }
    }
    throw new UnknownAuthenticationTypeException(authentication);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EncryptionStrategy getEncryptionStrategy(String name) {
    return cryptoStrategies.get(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addEncryptionStrategy(EncryptionStrategy strategy) {
    cryptoStrategies.put(strategy.getName(), strategy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EncryptionStrategy removeEncryptionStrategy(String name) {
    return cryptoStrategies.remove(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<EncryptionStrategy> getEncryptionStrategies() {
    List<EncryptionStrategy> allStrategies = new ArrayList<>(cryptoStrategies.values());
    return Collections.unmodifiableCollection(allStrategies);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setEncryptionStrategies(Collection<EncryptionStrategy> strategies) {
    for (EncryptionStrategy strategy : strategies) {
      addEncryptionStrategy(strategy);
    }
  }
}
