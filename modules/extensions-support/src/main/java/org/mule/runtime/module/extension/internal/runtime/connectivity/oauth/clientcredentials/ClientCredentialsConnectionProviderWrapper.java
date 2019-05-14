/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * A {@link ReconnectableConnectionProviderWrapper} which makes sure that by the time the
 * {@link ConnectionProvider#connect()} method is invoked on the delegate, the authorization dance has
 * been completed and the {@link ClientCredentialsState} and {@link OAuthCallbackValue} fields have
 * been properly injected
 *
 * @since 4.2.1
 */
public class ClientCredentialsConnectionProviderWrapper<C> extends OAuthConnectionProviderWrapper<C> {

  private final ClientCredentialsConfig oauthConfig;

  private final ClientCredentialsOAuthHandler oauthHandler;
  private final FieldSetter<ConnectionProvider<C>, ClientCredentialsState> oauthStateSetter;
  private final RunOnce dance;

  private ClientCredentialsOAuthDancer dancer;

  public ClientCredentialsConnectionProviderWrapper(ConnectionProvider<C> delegate,
                                                    ClientCredentialsConfig oauthConfig,
                                                    Map<Field, String> callbackValues,
                                                    ClientCredentialsOAuthHandler oauthHandler,
                                                    ReconnectionConfig reconnectionConfig) {
    super(delegate, reconnectionConfig, callbackValues);
    this.oauthConfig = oauthConfig;
    this.oauthHandler = oauthHandler;
    oauthStateSetter = getOAuthStateSetter(delegate, ClientCredentialsState.class, "Client Credentials");
    dance = Once.of(this::updateOAuthState);
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
        updatedContext -> updateOAuthParameters(delegate, updatedContext)));

    updateOAuthParameters(delegate, context);
  }

  private ResourceOwnerOAuthContext getContext() {
    return oauthHandler.getOAuthContext(oauthConfig);
  }

  @Override
  public void start() throws MuleException {
    dancer = oauthHandler.register(oauthConfig);
    super.start();
  }
}
