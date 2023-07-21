/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.security.adapter;

import org.mule.runtime.api.security.Authentication;

import java.util.Map;

/**
 * Adapts a {@link org.mule.sdk.api.security.Authentication} into a {@link Authentication}
 *
 * @since 4.5.0
 */
public class MuleAuthenticationAdapter implements Authentication {

  private final org.mule.sdk.api.security.Authentication delegate;

  public MuleAuthenticationAdapter(org.mule.sdk.api.security.Authentication delegate) {
    this.delegate = delegate;
  }

  @Override
  public Object getCredentials() {
    return delegate.getCredentials();
  }

  @Override
  public Object getPrincipal() {
    return delegate.getPrincipal();
  }

  @Override
  public Map<String, Object> getProperties() {
    return delegate.getProperties();
  }

  @Override
  public Authentication setProperties(Map<String, Object> properties) {
    delegate.setProperties(properties);
    return this;
  }
}
