/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.AuthorizationCodeOAuthDancer;
import org.mule.oauth.client.api.listener.AuthorizationCodeListener;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.OAuthCallbackConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.UpdatingAuthorizationCodeState;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.exception.TokenInvalidatedException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SmallTest
public class UpdatingAuthorizationCodeStateTestCase extends AbstractMuleTestCase {

  private static final String ACCESS_TOKEN = "myToken";
  private static final String REFRESH_TOKEN = "myRefreshToken";
  private static final String NEW_TOKEN = "newToken";
  private static final String NEW_REFRESH_TOKEN = "newRefresh";
  private static final String RESOURCE_OWNER_ID = "exp";
  private static final String EXPIRES_IN = "expires_in";
  private static final String STATE = "state";
  private static final String CONSUMER_KEY = "key";
  private static final String CONSUMER_SECRET = "secret";
  private static final String EXTERNAL_CALLBACK_URL = "externalCallbackUrl";
  private static final String ACCESS_TOKEN_URL = "accesstokenurl";
  private static final String AUTHORIZATION_URL = "AuthorizationUrl";

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  private AuthorizationCodeConfig oAuthConfig;

  @Mock
  private AuthorizationCodeOAuthDancer dancer;

  @Mock
  private ResourceOwnerOAuthContext initialContext;

  @Mock
  private ResourceOwnerOAuthContext refreshedContext;

  @Mock
  private OAuthCallbackConfig mockOAuthCallbackConfig;

  @Before
  public void before() {
    oAuthConfig = new AuthorizationCodeConfig("configName",
                                              empty(),
                                              new CustomOAuthParameters(),
                                              emptyMap(),
                                              new AuthorizationCodeGrantType(ACCESS_TOKEN_URL, AUTHORIZATION_URL, "#[s]", "reg",
                                                                             "#[x]", "sd"),
                                              new OAuthCallbackConfig("", "", "", EXTERNAL_CALLBACK_URL),
                                              CONSUMER_KEY, CONSUMER_SECRET, AUTHORIZATION_URL, ACCESS_TOKEN_URL, "scope",
                                              RESOURCE_OWNER_ID, null,
                                              null);

    when(initialContext.getAccessToken()).thenReturn(ACCESS_TOKEN);
    when(initialContext.getRefreshToken()).thenReturn(REFRESH_TOKEN);
    when(initialContext.getResourceOwnerId()).thenReturn(RESOURCE_OWNER_ID);
    when(initialContext.getExpiresIn()).thenReturn(EXPIRES_IN);
    when(initialContext.getState()).thenReturn(STATE);

    when(refreshedContext.getAccessToken()).thenReturn(NEW_TOKEN);
    when(refreshedContext.getRefreshToken()).thenReturn(NEW_REFRESH_TOKEN);
    when(refreshedContext.getResourceOwnerId()).thenReturn(RESOURCE_OWNER_ID);
    when(refreshedContext.getExpiresIn()).thenReturn(EXPIRES_IN);
  }

  @Test
  public void onRefreshToken() {
    ArgumentCaptor<AuthorizationCodeListener> listenerCaptor = forClass(AuthorizationCodeListener.class);
    Reference<ResourceOwnerOAuthContext> newContext = new Reference<>();

    UpdatingAuthorizationCodeState state = new UpdatingAuthorizationCodeState(oAuthConfig,
                                                                              dancer,
                                                                              initialContext,
                                                                              newContext::set, false);

    verify(dancer).addListener(anyString(), listenerCaptor.capture());

    assertThat(state.getAccessToken(), equalTo(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));
    assertThat(state.getExternalCallbackUrl().get(), equalTo(EXTERNAL_CALLBACK_URL));
    assertThat(state.getState().get(), equalTo(STATE));
    assertThat(state.getExpiresIn().get(), equalTo(EXPIRES_IN));
    assertThat(state.getConsumerKey(), equalTo("key"));
    assertThat(state.getConsumerSecret(), equalTo("secret"));
    assertThat(state.getAuthorizationUrl(), equalTo(AUTHORIZATION_URL));
    assertThat(state.getAccessTokenUrl(), equalTo(ACCESS_TOKEN_URL));

    AuthorizationCodeListener listener = listenerCaptor.getValue();
    assertTokenRefreshed(newContext, state, listener);
  }

  @Test
  public void onTokenInvalidated() {
    ArgumentCaptor<AuthorizationCodeListener> listenerCaptor = forClass(AuthorizationCodeListener.class);
    Reference<ResourceOwnerOAuthContext> newContext = new Reference<>();

    UpdatingAuthorizationCodeState state = new UpdatingAuthorizationCodeState(oAuthConfig,
                                                                              dancer,
                                                                              initialContext,
                                                                              newContext::set, false);

    verify(dancer).addListener(anyString(), listenerCaptor.capture());

    assertThat(state.getAccessToken(), equalTo(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));

    AuthorizationCodeListener listener = listenerCaptor.getValue();
    listener.onTokenInvalidated();

    try {
      state.getAccessToken();
      fail("This should have failed");
    } catch (TokenInvalidatedException e) {
      // carry on... nothing to see here
    }
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));
    assertThat(newContext.get(), is(nullValue()));
  }

  @Test
  public void onTokenUpdatedAfterInvalidation() {
    ArgumentCaptor<AuthorizationCodeListener> listenerCaptor = forClass(AuthorizationCodeListener.class);
    Reference<ResourceOwnerOAuthContext> newContext = new Reference<>();

    UpdatingAuthorizationCodeState state = new UpdatingAuthorizationCodeState(oAuthConfig,
                                                                              dancer,
                                                                              initialContext,
                                                                              newContext::set, false);

    verify(dancer).addListener(anyString(), listenerCaptor.capture());

    assertThat(state.getAccessToken(), equalTo(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));

    AuthorizationCodeListener listener = listenerCaptor.getValue();
    listener.onTokenInvalidated();

    try {
      state.getAccessToken();
      fail("This should have failed");
    } catch (TokenInvalidatedException e) {
      // carry on... nothing to see here
    }

    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));
    assertTokenRefreshed(newContext, state, listener);
  }

  @Test
  public void onReAuthorization() {
    ArgumentCaptor<AuthorizationCodeListener> listenerCaptor = forClass(AuthorizationCodeListener.class);
    Reference<ResourceOwnerOAuthContext> newContext = new Reference<>();

    UpdatingAuthorizationCodeState state = new UpdatingAuthorizationCodeState(oAuthConfig,
                                                                              dancer,
                                                                              initialContext,
                                                                              newContext::set, false);

    verify(dancer).addListener(anyString(), listenerCaptor.capture());

    assertThat(state.getAccessToken(), equalTo(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));

    AuthorizationCodeListener listener = listenerCaptor.getValue();
    listener.onAuthorizationCompleted(refreshedContext);

    assertThat(state.getAccessToken(), equalTo(NEW_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(NEW_REFRESH_TOKEN));

    assertThat(newContext.get(), is(sameInstance(refreshedContext)));
  }

  private void assertTokenRefreshed(Reference<ResourceOwnerOAuthContext> newContext, UpdatingAuthorizationCodeState state,
                                    AuthorizationCodeListener listener) {
    listener.onTokenRefreshed(refreshedContext);

    assertThat(state.getAccessToken(), equalTo(NEW_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(NEW_REFRESH_TOKEN));

    assertThat(newContext.get(), is(sameInstance(refreshedContext)));
  }

  @Test
  @Issue("W-15154658")
  public void accessTokenVisitsTokensStoreIfClusterIsEnabled() throws Exception {
    ArgumentCaptor<AuthorizationCodeListener> listenerCaptor = forClass(AuthorizationCodeListener.class);
    Reference<ResourceOwnerOAuthContext> newContext = new Reference<>();

    UpdatingAuthorizationCodeState state = new UpdatingAuthorizationCodeState(oAuthConfig,
                                                                              dancer,
                                                                              initialContext,
                                                                              newContext::set, true);

    verify(dancer).addListener(anyString(), listenerCaptor.capture());

    assertThat(state.getAccessToken(), equalTo(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));

    // with cluster being enabled, getAccessToken() will call getInvalidateFromTokensStore method in dancer
    verify(dancer, times(1)).getInvalidateFromTokensStore(any());
  }

  @Test
  @Issue("W-15154658")
  public void accessTokenVisitsTokensStoreWithoutCluster() throws Exception {
    ArgumentCaptor<AuthorizationCodeListener> listenerCaptor = forClass(AuthorizationCodeListener.class);
    Reference<ResourceOwnerOAuthContext> newContext = new Reference<>();

    UpdatingAuthorizationCodeState state = new UpdatingAuthorizationCodeState(oAuthConfig,
                                                                              dancer,
                                                                              initialContext,
                                                                              newContext::set, false);

    verify(dancer).addListener(anyString(), listenerCaptor.capture());

    assertThat(state.getAccessToken(), equalTo(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));

    // with cluster not being enabled, getAccessToken() won't call getInvalidateFromTokensStore method in dancer
    verify(dancer, never()).getInvalidateFromTokensStore(any());
  }
}
