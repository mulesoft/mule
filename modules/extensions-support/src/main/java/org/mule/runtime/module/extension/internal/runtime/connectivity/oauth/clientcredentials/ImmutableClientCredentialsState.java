/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static java.util.Optional.ofNullable;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;

import java.util.Optional;

public class ImmutableClientCredentialsState implements ClientCredentialsState {

  private final String accessToken;
  private final String refreshToken;
  private final String expiresIn;

  public ImmutableClientCredentialsState(String accessToken, String refreshToken, String expiresIn) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiresIn = expiresIn;
  }

  @Override
  public String getAccessToken() {
    return accessToken;
  }

  @Override
  public Optional<String> getRefreshToken() {
    return ofNullable(refreshToken);
  }

  @Override
  public Optional<String> getExpiresIn() {
    return ofNullable(expiresIn);
  }
}
