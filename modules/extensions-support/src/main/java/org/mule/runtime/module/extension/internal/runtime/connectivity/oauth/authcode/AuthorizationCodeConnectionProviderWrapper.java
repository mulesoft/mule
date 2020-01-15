/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.getOAuthStateSetter;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * A {@link ReconnectableConnectionProviderWrapper} which makes sure that by the time the
 * {@link ConnectionProvider#connect()} method is invoked on the delegate, the authorization dance has
 * been completed and the {@link AuthorizationCodeState} and {@link OAuthCallbackValue} fields have
 * been properly injected
 *
 * @since 4.0
 */
public class AuthorizationCodeConnectionProviderWrapper<C> extends OAuthConnectionProviderWrapper<C>
    implements NoConnectivityTest {

  private final AuthorizationCodeConfig oauthConfig;
  private final AuthorizationCodeOAuthHandler oauthHandler;
  private final FieldSetter<ConnectionProvider<C>, AuthorizationCodeState> authCodeStateSetter;
  private final RunOnce dance;

  private AuthorizationCodeOAuthDancer dancer;

  public AuthorizationCodeConnectionProviderWrapper(ConnectionProvider<C> delegate,
                                                    AuthorizationCodeConfig oauthConfig,
                                                    Map<Field, String> callbackValues,
                                                    AuthorizationCodeOAuthHandler oauthHandler,
                                                    ReconnectionConfig reconnectionConfig) {
    super(delegate, reconnectionConfig, callbackValues);
    this.oauthConfig = oauthConfig;
    this.oauthHandler = oauthHandler;
    authCodeStateSetter = getOAuthStateSetter(delegate, AuthorizationCodeState.class, oauthConfig.getGrantType());
    dance = Once.of(this::updateAuthState);
  }

  @Override
  public C connect() throws ConnectionException {
    dance.runOnce();
    return super.connect();
  }

  private void updateAuthState() {
    final ConnectionProvider<C> delegate = getDelegate();
    ResourceOwnerOAuthContext context = getContext();
    authCodeStateSetter
        .set(delegate, new UpdatingAuthorizationCodeState(oauthConfig, dancer, context,
                                                          updatedContext -> updateOAuthParameters(delegate, updatedContext)));
    updateOAuthParameters(delegate, context);
  }

  @Override
  public void refreshToken(String resourceOwnerId) {
    oauthHandler.refreshToken(oauthConfig.getOwnerConfigName(), resourceOwnerId);
  }

  @Override
  public void invalidate(String resourceOwnerId) {
    oauthHandler.invalidate(oauthConfig.getOwnerConfigName(), resourceOwnerId);
  }

  @Override
  public OAuthGrantType getGrantType() {
    return oauthConfig.getGrantType();
  }

  public String getResourceOwnerId() {
    return getContext().getResourceOwnerId();
  }

  private ResourceOwnerOAuthContext getContext() {
    return oauthHandler.getOAuthContext(oauthConfig)
        .orElseThrow(() -> new IllegalArgumentException("OAuth authorization dance not yet performed for resourceOwnerId "
            + oauthConfig.getResourceOwnerId()));
  }

  @Override
  public void start() throws MuleException {
    dancer = oauthHandler.register(oauthConfig);
    super.start();
  }
}
