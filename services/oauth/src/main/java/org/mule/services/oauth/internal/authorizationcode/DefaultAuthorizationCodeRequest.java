/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal.authorizationcode;

import static java.util.Optional.ofNullable;

import org.mule.runtime.oauth.api.AuthorizationCodeRequest;

import java.util.Optional;

/**
 * Immutable implementation of {@link AuthorizationCodeRequest}.
 * 
 * @since 4.0
 */
public class DefaultAuthorizationCodeRequest implements AuthorizationCodeRequest {

  private String resourceOwnerId;
  private String authorizationUrl;
  private String tokenUrl;
  private String clientId;
  private String clientSecret;
  private String scopes;
  private String state;

  public DefaultAuthorizationCodeRequest(String resourceOwnerId, String authorizationUrl, String tokenUrl, String clientId,
                                         String clientSecret, String scopes, String state) {
    this.resourceOwnerId = resourceOwnerId;
    this.authorizationUrl = authorizationUrl;
    this.tokenUrl = tokenUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.scopes = scopes;
    this.state = state;
  }

  @Override
  public String getResourceOwnerId() {
    return resourceOwnerId;
  }

  @Override
  public String getAuthorizationUrl() {
    return authorizationUrl;
  }

  @Override
  public String getTokenUrl() {
    return tokenUrl;
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  @Override
  public String getClientSecret() {
    return clientSecret;
  }

  @Override
  public String getScopes() {
    return scopes;
  }

  @Override
  public Optional<String> getState() {
    return ofNullable(state);
  }

}
