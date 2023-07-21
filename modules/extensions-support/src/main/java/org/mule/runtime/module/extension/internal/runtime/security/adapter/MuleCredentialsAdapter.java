/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.security.adapter;

import org.mule.runtime.api.security.Credentials;

/**
 * Adapts a {@link org.mule.sdk.api.security.Credentials} into a {@link Credentials}
 *
 * @since 4.5.0
 */
public class MuleCredentialsAdapter implements Credentials {

  private final org.mule.sdk.api.security.Credentials delegate;

  public MuleCredentialsAdapter(org.mule.sdk.api.security.Credentials delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getUsername() {
    return delegate.getUsername();
  }

  @Override
  public char[] getPassword() {
    return delegate.getPassword();
  }

  @Override
  public Object getRoles() {
    return delegate.getRoles();
  }
}
