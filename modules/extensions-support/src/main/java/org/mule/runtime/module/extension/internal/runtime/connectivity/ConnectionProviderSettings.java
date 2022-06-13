/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials.ClientCredentialsOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs.PlatformManagedOAuthHandler;

import java.util.Map;
import java.util.Optional;

public class ConnectionProviderSettings {

  private final ConnectionProviderModel connectionProviderModel;
  private final Map<String, Object> parameters;
  private final Optional<PoolingProfile> poolingProfile;
  private final Optional<ReconnectionConfig> reconnectionConfig;
  private final AuthorizationCodeOAuthHandler authorizationCodeOAuthHandler;
  private final ClientCredentialsOAuthHandler clientCredentialsOAuthHandler;
  private final PlatformManagedOAuthHandler platformManagedOAuthHandler;

  public ConnectionProviderSettings(ConnectionProviderModel connectionProviderModel,
                                    Map<String, Object> parameters,
                                    PoolingProfile poolingProfile,
                                    ReconnectionConfig reconnectionConfig,
                                    AuthorizationCodeOAuthHandler authorizationCodeOAuthHandler,
                                    ClientCredentialsOAuthHandler clientCredentialsOAuthHandler,
                                    PlatformManagedOAuthHandler platformManagedOAuthHandler) {
    this.connectionProviderModel = connectionProviderModel;
    this.parameters = unmodifiableMap(parameters);
    this.poolingProfile = ofNullable(poolingProfile);
    this.reconnectionConfig = ofNullable(reconnectionConfig);
    this.authorizationCodeOAuthHandler = authorizationCodeOAuthHandler;
    this.clientCredentialsOAuthHandler = clientCredentialsOAuthHandler;
    this.platformManagedOAuthHandler = platformManagedOAuthHandler;
  }

  public ConnectionProviderModel getConnectionProviderModel() {
    return connectionProviderModel;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public Optional<PoolingProfile> getPoolingProfile() {
    return poolingProfile;
  }

  public Optional<ReconnectionConfig> getReconnectionConfig() {
    return reconnectionConfig;
  }

  public AuthorizationCodeOAuthHandler getAuthorizationCodeOAuthHandler() {
    return authorizationCodeOAuthHandler;
  }

  public ClientCredentialsOAuthHandler getClientCredentialsOAuthHandler() {
    return clientCredentialsOAuthHandler;
  }

  public PlatformManagedOAuthHandler getPlatformManagedOAuthHandler() {
    return platformManagedOAuthHandler;
  }
}
