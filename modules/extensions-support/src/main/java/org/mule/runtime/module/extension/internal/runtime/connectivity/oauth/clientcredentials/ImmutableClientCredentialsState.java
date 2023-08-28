/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static java.util.Optional.ofNullable;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;

import java.util.Optional;

/**
 * Immutable implementation of {@link ClientCredentialsState}
 *
 * @since 4.2.1
 */
public class ImmutableClientCredentialsState implements ClientCredentialsState {

  private final String accessToken;
  private final String expiresIn;

  public ImmutableClientCredentialsState(String accessToken, String expiresIn) {
    this.accessToken = accessToken;
    this.expiresIn = expiresIn;
  }

  @Override
  public String getAccessToken() {
    return accessToken;
  }

  @Override
  public Optional<String> getExpiresIn() {
    return ofNullable(expiresIn);
  }
}
