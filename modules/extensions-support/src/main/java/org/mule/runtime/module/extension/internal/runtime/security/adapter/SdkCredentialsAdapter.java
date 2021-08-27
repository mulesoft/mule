/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.security.adapter;

import org.mule.sdk.api.security.Credentials;

/**
 *
 *
 * @since 4.4.0
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
