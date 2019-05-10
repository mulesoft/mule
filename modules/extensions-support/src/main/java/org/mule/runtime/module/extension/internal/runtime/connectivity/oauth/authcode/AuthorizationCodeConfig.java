/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.MultiMap.emptyMultiMap;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthObjectStoreConfig;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the values used to configure an extension to consume a particular OAuth provider using
 * the Authorization-Code grant type
 *
 * @since 4.0
 */
public final class AuthorizationCodeConfig extends OAuthConfig<AuthorizationCodeGrantType> {

  private final String consumerKey;
  private final String consumerSecret;
  private final String authorizationUrl;
  private final String accessTokenUrl;
  private final String scope;
  private final String resourceOwnerId;
  private final String before;
  private final String after;
  private final OAuthCallbackConfig callbackConfig;
  private final AuthorizationCodeGrantType grantType;


  public AuthorizationCodeConfig(String ownerConfigName,
                                 Optional<OAuthObjectStoreConfig> storeConfig,
                                 MultiMap<String, String> customParameters,
                                 Map<Field, String> parameterExtractors,
                                 AuthorizationCodeGrantType grantType,
                                 OAuthCallbackConfig callbackConfig,
                                 String consumerKey,
                                 String consumerSecret,
                                 String authorizationUrl,
                                 String accessTokenUrl,
                                 String scope,
                                 String resourceOwnerId,
                                 String before,
                                 String after) {
    super(ownerConfigName, storeConfig, customParameters, emptyMultiMap(), parameterExtractors);

    this.consumerKey = consumerKey;
    this.consumerSecret = consumerSecret;
    this.authorizationUrl = authorizationUrl;
    this.accessTokenUrl = accessTokenUrl;
    this.scope = scope;
    this.resourceOwnerId = resourceOwnerId;
    this.before = before;
    this.after = after;
    this.callbackConfig = callbackConfig;
    this.grantType = grantType;
  }

  @Override
  public AuthorizationCodeGrantType getGrantType() {
    return grantType;
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

  public OAuthCallbackConfig getCallbackConfig() {
    return callbackConfig;
  }
}
