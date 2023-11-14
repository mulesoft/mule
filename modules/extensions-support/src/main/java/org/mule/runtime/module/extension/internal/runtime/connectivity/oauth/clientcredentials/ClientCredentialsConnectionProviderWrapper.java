/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.CLIENT_CREDENTIALS_STATE_INTERFACES;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.getOAuthStateSetter;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.updateOAuthParameters;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.BaseOAuthConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.oauth.client.api.ClientCredentialsOAuthDancer;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * A {@link ReconnectableConnectionProviderWrapper} which makes sure that by the time the {@link ConnectionProvider#connect()}
 * method is invoked on the delegate, the authorization dance has been completed and the {@link ClientCredentialsState} and
 * {@link OAuthCallbackValue} fields have been properly injected
 *
 * @since 4.2.1
 */
public class ClientCredentialsConnectionProviderWrapper<C> extends BaseOAuthConnectionProviderWrapper<C> {

  private final ClientCredentialsConfig oauthConfig;

  private final ClientCredentialsOAuthHandler oauthHandler;
  private final FieldSetter<Object, Object> oauthStateSetter;
  private final RunOnce dance;

  private ClientCredentialsOAuthDancer dancer;
  private UpdatingClientCredentialsState updatingClientCredentialsState;

  public ClientCredentialsConnectionProviderWrapper(ConnectionProvider<C> delegate,
                                                    ClientCredentialsConfig oauthConfig,
                                                    Map<Field, String> callbackValues,
                                                    ClientCredentialsOAuthHandler oauthHandler,
                                                    ReconnectionConfig reconnectionConfig) {
    super(delegate, reconnectionConfig, callbackValues);
    this.oauthConfig = oauthConfig;
    this.oauthHandler = oauthHandler;
    oauthStateSetter = resolveOauthStateSetter(oauthConfig);
    dance = Once.of(this::updateOAuthState);
  }

  @Override
  public C connect() throws ConnectionException {
    dance.runOnce();
    return super.connect();
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
    final Object delegate = getDelegateForInjection();
    ResourceOwnerOAuthContext context = getContext();
    updatingClientCredentialsState = new UpdatingClientCredentialsState(
                                                                        dancer,
                                                                        context,
                                                                        updatedContext -> updateOAuthParameters(delegate,
                                                                                                                callbackValues,
                                                                                                                updatedContext));
    oauthStateSetter.set(delegate, updatingClientCredentialsState);
    updateOAuthParameters(delegate, callbackValues, context);
  }

  @Override
  protected ResourceOwnerOAuthContext getContext() {
    return oauthHandler.getOAuthContext(oauthConfig);
  }

  @Override
  public void start() throws MuleException {
    dancer = oauthHandler.register(oauthConfig);
    super.start();
  }

  @Override
  public void stop() throws MuleException {
    if (updatingClientCredentialsState != null) {
      updatingClientCredentialsState.deRegisterListener();
    }
    super.stop();
  }

  protected FieldSetter<Object, Object> resolveOauthStateSetter(ClientCredentialsConfig oauthConfig) {
    return getOAuthStateSetter(getDelegateForInjection(), CLIENT_CREDENTIALS_STATE_INTERFACES, oauthConfig.getGrantType());
  }

}
