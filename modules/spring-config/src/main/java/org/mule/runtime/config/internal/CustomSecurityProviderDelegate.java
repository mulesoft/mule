/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityProvider;

/**
 * Delegate for security providers configured inside security-manager
 *
 * @since 4.0
 */
public class CustomSecurityProviderDelegate extends AbstractComponent implements SecurityProvider, Initialisable {

  private SecurityProvider delegate;

  public CustomSecurityProviderDelegate(SecurityProvider delegate, String name) {
    this.delegate = delegate;
    this.delegate.setName(name);
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public void setName(String name) {
    delegate.setName(name);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(delegate);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws SecurityException {
    return delegate.authenticate(authentication);
  }

  @Override
  public boolean supports(Class<?> aClass) {
    return delegate.supports(aClass);
  }

  @Override
  public SecurityContext createSecurityContext(Authentication auth) throws UnknownAuthenticationTypeException {
    return delegate.createSecurityContext(auth);
  }
}
