/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.security.adapter;

import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.module.extension.internal.runtime.security.DefaultCredentialsBuilder;
import org.mule.sdk.api.security.Authentication;
import org.mule.sdk.api.security.AuthenticationHandler;
import org.mule.sdk.api.security.Credentials;
import org.mule.sdk.api.security.CredentialsBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.security.AuthenticationHandler} into a {@link AuthenticationHandler}
 *
 * @since 4.4.0
 */
public class SdkAuthenticationHandlerAdapter implements AuthenticationHandler {

  private final org.mule.runtime.extension.api.security.AuthenticationHandler delegate;

  public SdkAuthenticationHandlerAdapter(org.mule.runtime.extension.api.security.AuthenticationHandler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setAuthentication(Authentication authentication)
      throws SecurityProviderNotFoundException, SecurityException, UnknownAuthenticationTypeException {
    delegate.setAuthentication(new MuleAuthenticationAdapter(authentication));
  }

  @Override
  public void setAuthentication(List<String> list, Authentication authentication)
      throws SecurityProviderNotFoundException, SecurityException, UnknownAuthenticationTypeException {
    delegate.setAuthentication(list, new MuleAuthenticationAdapter(authentication));
  }

  @Override
  public Optional<Authentication> getAuthentication() {
    return delegate.getAuthentication().map(SdkAuthenticationAdapter::new);
  }

  @Override
  public Authentication createAuthentication(Credentials credentials) {
    return new SdkAuthenticationAdapter(delegate.createAuthentication(new MuleCredentialsAdapter(credentials)));
  }

  @Override
  public CredentialsBuilder createCredentialsBuilder() {
    return new SdkCredentialsBuilderAdapter(delegate.createCredentialsBuilder());
  }
}
