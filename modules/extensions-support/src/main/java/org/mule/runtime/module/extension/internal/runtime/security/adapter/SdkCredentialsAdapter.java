/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.security.adapter;

import org.mule.sdk.api.security.Credentials;

/**
 *
 *
 * @since 4.5.0
 */
public class SdkCredentialsAdapter implements Credentials {

  private final org.mule.runtime.api.security.Credentials delegate;

  public SdkCredentialsAdapter(org.mule.runtime.api.security.Credentials delegate) {
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
