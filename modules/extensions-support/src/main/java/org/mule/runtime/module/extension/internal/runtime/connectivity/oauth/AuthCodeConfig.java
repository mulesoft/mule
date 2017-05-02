/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.util.Optional.ofNullable;

import java.util.Optional;

/**
 * Holds the values used to configure an extension to consume a particular OAuth provider using
 * the Authorization-Code grant type
 *
 * @since 4.0
 */
public final class AuthCodeConfig {

  private final String consumerKey;
  private final String consumerSecret;
  private final String authorizationUrl;
  private final String accessTokenUrl;
  private final String scope;
  private final String resourceOwnerId;
  private final String before;
  private final String after;

  public AuthCodeConfig(String consumerKey, String consumerSecret, String authorizationUrl, String accessTokenUrl,
                        String scope, String resourceOwnerId, String before, String after) {
    this.consumerKey = consumerKey;
    this.consumerSecret = consumerSecret;
    this.authorizationUrl = authorizationUrl;
    this.accessTokenUrl = accessTokenUrl;
    this.scope = scope;
    this.resourceOwnerId = resourceOwnerId;
    this.before = before;
    this.after = after;
  }

  public String getConsumerKey() {
    return consumerKey;
  }

  public String getConsumerSecret() {
    return consumerSecret;
  }

  public String getAuthorizationUrl() {
    return authorizationUrl;
  }

  public String getAccessTokenUrl() {
    return accessTokenUrl;
  }

  public Optional<String> getScope() {
    return ofNullable(scope);
  }

  public String getResourceOwnerId() {
    return resourceOwnerId;
  }

  public Optional<String> getBefore() {
    return ofNullable(before);
  }

  public Optional<String> getAfter() {
    return ofNullable(after);
  }
}
