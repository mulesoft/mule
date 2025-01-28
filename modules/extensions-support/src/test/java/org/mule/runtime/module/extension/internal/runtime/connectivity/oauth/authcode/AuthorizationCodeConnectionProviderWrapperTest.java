/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.AuthorizationCodeOAuthDancer;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.module.extension.internal.util.FieldSetter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

public class AuthorizationCodeConnectionProviderWrapperTest {

  private AuthorizationCodeOAuthHandler oauthHandler;
  private AuthorizationCodeGrantType type;
  private AuthorizationCodeConnectionProviderWrapper wrapper;

  @Test
  public void listenerIsRegistered() throws MuleException {
    setupCommonMocks();
    AuthorizationCodeOAuthDancer dancer = mock(AuthorizationCodeOAuthDancer.class);
    when(oauthHandler.register(any())).thenReturn(dancer);

    wrapper.start();
    wrapper.connect();
    verify(dancer, times(1)).addListener(any(), any());
  }

  @Test
  public void refreshTokenCallsOauthHandler() throws MuleException {
    setupCommonMocks();
    wrapper.refreshToken("id1");
    verify(oauthHandler, times(1)).refreshToken(any(), any());
  }

  @Test
  public void invalidateCallsOauthHandler() throws MuleException {
    setupCommonMocks();
    wrapper.invalidate("id1");
    verify(oauthHandler, times(1)).invalidate(any(), any());
  }

  @Test
  public void getOAuthGrantType() throws MuleException {
    setupCommonMocks();
    assertThat(wrapper.getGrantType(), is(type));
  }

  private void setupCommonMocks() throws ConnectionException {
    ConnectionProvider delegate = mock(ConnectionProvider.class);
    when(delegate.connect()).thenReturn(new Object());

    AuthorizationCodeConfig oauthConfig = mock(AuthorizationCodeConfig.class);
    this.type = new AuthorizationCodeGrantType("http://accessToken",
                                               "http://auth",
                                               "#[accessToken]",
                                               ".*",
                                               "#[refreshToken]",
                                               null);
    when(oauthConfig.getGrantType()).thenReturn(type);
    when(oauthConfig.getCallbackConfig()).thenReturn(mock(OAuthCallbackConfig.class));
    Map<Field, String> callbackValues = new HashMap<>();
    this.oauthHandler = mock(AuthorizationCodeOAuthHandler.class);
    when(oauthHandler.getOAuthContext(any())).thenReturn(Optional.ofNullable(mock(ResourceOwnerOAuthContext.class)));
    ReconnectionConfig reconnectionConfig = mock(ReconnectionConfig.class);
    this.wrapper =
        new AuthorizationCodeConnectionProviderWrapperTest.TestAuthorizationCodeConnectionProviderWrapper(delegate, oauthConfig,
                                                                                                          callbackValues,
                                                                                                          oauthHandler,
                                                                                                          reconnectionConfig);
  }

  private class TestAuthorizationCodeConnectionProviderWrapper extends AuthorizationCodeConnectionProviderWrapper {

    public TestAuthorizationCodeConnectionProviderWrapper(ConnectionProvider delegate, AuthorizationCodeConfig oauthConfig,
                                                          Map<Field, String> callbackValues,
                                                          AuthorizationCodeOAuthHandler oauthHandler,
                                                          ReconnectionConfig reconnectionConfig) {
      super(delegate, oauthConfig, callbackValues, oauthHandler, reconnectionConfig, () -> false);
    }

    @Override
    protected FieldSetter<Object, Object> resolveOauthStateSetter(AuthorizationCodeConfig oauthConfig) {
      return mock(FieldSetter.class);
    }
  }
}
