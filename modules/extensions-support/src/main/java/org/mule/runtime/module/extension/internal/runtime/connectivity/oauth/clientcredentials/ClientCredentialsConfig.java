/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static java.util.Optional.ofNullable;

import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.CustomOAuthParameters;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthObjectStoreConfig;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * {@link OAuthConfig} specialization for client credentials grant type
 *
 * @since 4.2.1
 */
public class ClientCredentialsConfig extends OAuthConfig<ClientCredentialsGrantType> {

  private static final String CONFIG_ID_SEPARATOR = "//";

  private final String clientId;
  private final String clientSecret;
  private final String tokenUrl;
  private final String scope;
  private final CredentialsPlacement credentialsPlacement;
  private final ClientCredentialsGrantType grantType;
  private final String configIdentifier;

  public ClientCredentialsConfig(String ownerConfigName,
                                 Optional<OAuthObjectStoreConfig> storeConfig,
                                 CustomOAuthParameters customOAuthParameters,
                                 Map<Field, String> parameterExtractors,
                                 String clientId,
                                 String clientSecret,
                                 String tokenUrl,
                                 String scope,
                                 CredentialsPlacement credentialsPlacement,
                                 ClientCredentialsGrantType grantType) {
    super(ownerConfigName, storeConfig, customOAuthParameters, parameterExtractors);
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tokenUrl = tokenUrl;
    this.scope = scope;
    this.credentialsPlacement = credentialsPlacement;
    this.grantType = grantType;
    this.configIdentifier = generateConfigIdentifier();
  }

  private String generateConfigIdentifier() {
    StringBuilder configIdentifierBuilder = new StringBuilder();
    configIdentifierBuilder.append(getOwnerConfigName());
    configIdentifierBuilder.append(CONFIG_ID_SEPARATOR);
    configIdentifierBuilder.append(getClientId());
    configIdentifierBuilder.append(CONFIG_ID_SEPARATOR);
    configIdentifierBuilder.append(getClientSecret());
    configIdentifierBuilder.append(CONFIG_ID_SEPARATOR);
    configIdentifierBuilder.append(getTokenUrl());
    configIdentifierBuilder.append(CONFIG_ID_SEPARATOR);
    configIdentifierBuilder.append(getScope().orElse(""));
    return configIdentifierBuilder.toString();
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

  public String getConfigIdentifier() {
    return configIdentifier;
  }

  @Override
  public ClientCredentialsGrantType getGrantType() {
    return grantType;
  }
}
