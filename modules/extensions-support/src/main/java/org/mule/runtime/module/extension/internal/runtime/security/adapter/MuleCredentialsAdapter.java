/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.security.adapter;

import org.mule.runtime.api.security.Credentials;

/**
 * Adapts a {@link org.mule.sdk.api.security.Credentials} into a {@link Credentials}
 *
 * @since 4.4.0
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
