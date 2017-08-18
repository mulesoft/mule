/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.connectivity.oauth;

import static java.util.Optional.ofNullable;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;

import java.util.Optional;

/**
 * Immutable implementation of {@link AuthCodeRequest}
 *
 * @see AuthCodeRequest
 * @since 4.0
 */
public class ImmutableAuthCodeRequest implements AuthCodeRequest {

  private final String resourceOwnerId;
  private final String scopes;
  private final String state;
  private final Optional<String> externalCallbackUrl;

  public ImmutableAuthCodeRequest(String resourceOwnerId, String scopes, String state, Optional<String> externalCallbackUrl) {
    this.resourceOwnerId = resourceOwnerId;
    this.scopes = scopes;
    this.state = state;
    this.externalCallbackUrl = externalCallbackUrl;
  }

  @Override
  public String getResourceOwnerId() {
    return resourceOwnerId;
  }

  @Override
  public Optional<String> getScopes() {
    return ofNullable(scopes);
  }

  @Override
  public Optional<String> getState() {
    return ofNullable(state);
  }

  @Override
  public Optional<String> getExternalCallbackUrl() {
    return externalCallbackUrl;
  }
}
