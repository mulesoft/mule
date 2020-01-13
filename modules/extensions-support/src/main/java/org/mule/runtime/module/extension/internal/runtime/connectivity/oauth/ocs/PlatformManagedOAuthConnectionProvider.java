/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ConnectionUtils;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials.UpdatingClientCredentialsState;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.Optional;

import javax.inject.Inject;

public class PlatformManagedOAuthConnectionProvider<C> implements ConnectionProviderWrapper<C> {

  private final PlatformManagedOAuthConfig oauthConfig;
  private final PlatformManagedOAuthHandler oauthHandler;
  private final Pair<ConnectionProviderModel, OAuthGrantType> delegateModel;
  private final PoolingProfile poolingProfile;
  private final ReconnectionConfig reconnectionConfig;

  private PlatformManagedOAuthDancer dancer;

  @Inject
  private MuleContext muleContext;

  public PlatformManagedOAuthConnectionProvider(PlatformManagedOAuthConfig oauthConfig,
                                                Pair<ConnectionProviderModel, OAuthGrantType> delegateModel,
                                                PlatformManagedOAuthHandler oauthHandler,
                                                ReconnectionConfig reconnectionConfig,
                                                PoolingProfile poolingProfile) {
    this.oauthConfig = oauthConfig;
    this.delegateModel = delegateModel;
    this.oauthHandler = oauthHandler;
    this.reconnectionConfig = reconnectionConfig;
    this.poolingProfile = poolingProfile;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(getRetryPolicyTemplate(), true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    dancer = oauthHandler.register(oauthConfig);

    //TODO: validate connectionId and get connection details

    

    startIfNeeded(getRetryPolicyTemplate());
  }

  @Override
  public C connect() throws ConnectionException {
    dance.runOnce();
    return super.connect();
  }

  @Override
  public ConnectionValidationResult validate(C connection) {
    try {
      ResourceOwnerOAuthContext context = getContext();
      if (context.getAccessToken() != null) {
        return getDelegate().validate(connection);
      } else {
        String message = "Server did not granted an access token";
        return failure(message, new IllegalStateException(message));
      }
    } catch (Exception e) {
      return failure("Could not obtain an access token", e);
    }
  }

  @Override
  public void refreshToken(String resourceOwnerId) {
    oauthHandler.refreshToken(oauthConfig);
  }

  @Override
  public void invalidate(String resourceOwnerId) {
    oauthHandler.invalidate(oauthConfig);
  }

  @Override
  public OAuthGrantType getGrantType() {
    return oauthConfig.getGrantType();
  }

  private void updateOAuthState() {
    final ConnectionProvider<C> delegate = getDelegate();
    ResourceOwnerOAuthContext context = getContext();
    oauthStateSetter.set(delegate, new UpdatingClientCredentialsState(
        dancer,
        context,
        updatedContext -> updateOAuthParameters(delegate,
                                                updatedContext)));

    updateOAuthParameters(delegate, context);
  }

  private ResourceOwnerOAuthContext getContext() {
    return oauthHandler.getOAuthContext(oauthConfig);
  }

  @Override
  public Optional<PoolingProfile> getPoolingProfile() {
    return ofNullable(poolingProfile);
  }

  @Override
  public Optional<ReconnectionConfig> getReconnectionConfig() {
    return ofNullable(reconnectionConfig);
  }

  @Override
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return ConnectionUtils.getRetryPolicyTemplate(getReconnectionConfig());
  }
}
