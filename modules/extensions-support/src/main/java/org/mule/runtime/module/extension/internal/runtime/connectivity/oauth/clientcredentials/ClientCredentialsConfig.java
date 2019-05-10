/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static java.util.Optional.ofNullable;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthObjectStoreConfig;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

public class ClientCredentialsConfig extends OAuthConfig<ClientCredentialsGrantType> {

  private final String clientId;
  private final String clientSecret;
  private final String tokenUrl;
  private final String scope;
  private final CredentialsPlacement credentialsPlacement;
  private final ClientCredentialsGrantType grantType;

  public ClientCredentialsConfig(String ownerConfigName,
                                 Optional<OAuthObjectStoreConfig> storeConfig,
                                 MultiMap<String, String> customParameters,
                                 MultiMap<String, String> customHeaders,
                                 Map<Field, String> parameterExtractors,
                                 String clientId,
                                 String clientSecret,
                                 String tokenUrl,
                                 String scope,
                                 CredentialsPlacement credentialsPlacement,
                                 ClientCredentialsGrantType grantType) {
    super(ownerConfigName, storeConfig, customParameters, customHeaders, parameterExtractors);
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tokenUrl = tokenUrl;
    this.scope = scope;
    this.credentialsPlacement = credentialsPlacement;
    this.grantType = grantType;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getTokenUrl() {
    return tokenUrl;
  }

  public CredentialsPlacement getCredentialsPlacement() {
    return credentialsPlacement;
  }

  public Optional<String> getScope() {
    return ofNullable(scope);
  }

  @Override
  public ClientCredentialsGrantType getGrantType() {
    return grantType;
  }
}
