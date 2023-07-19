/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.security.adapter;


import org.mule.sdk.api.security.Credentials;
import org.mule.sdk.api.security.CredentialsBuilder;

/**
 * Adapts a {@link org.mule.runtime.api.security.CredentialsBuilder} into a {@link CredentialsBuilder}
 *
 * @since 4.5.0
 */
public class SdkCredentialsBuilderAdapter implements CredentialsBuilder {

  private final org.mule.runtime.api.security.CredentialsBuilder delegate;

  public SdkCredentialsBuilderAdapter(org.mule.runtime.api.security.CredentialsBuilder delegate) {
    this.delegate = delegate;
  }

  @Override
  public CredentialsBuilder withUsername(String username) {
    delegate.withUsername(username);
    return this;
  }

  @Override
  public CredentialsBuilder withPassword(char[] password) {
    delegate.withPassword(password);
    return this;
  }

  @Override
  public CredentialsBuilder withRoles(Object roles) {
    delegate.withRoles(roles);
    return this;
  }

  @Override
  public Credentials build() {
    return new SdkCredentialsAdapter(delegate.build());
  }
}
