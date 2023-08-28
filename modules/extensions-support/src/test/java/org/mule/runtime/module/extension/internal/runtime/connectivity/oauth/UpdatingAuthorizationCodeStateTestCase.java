/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
  private static final String RESOURCE_OWNER_ID = "id";

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  private AuthorizationCodeConfig oAuthConfig;

  @Mock
  private AuthorizationCodeOAuthDancer dancer;

  @Mock
  private ResourceOwnerOAuthContext initialContext;

  @Mock
  private ResourceOwnerOAuthContext refreshedContext;

  @Before
  public void before() {
    oAuthConfig = new AuthorizationCodeConfig("configName",
                                              empty(),
                                              new CustomOAuthParameters(),
                                              emptyMap(),
                                              new AuthorizationCodeGrantType("url", "url", "#[s]", "reg", "#[x]", "sd"),
                                              mock(OAuthCallbackConfig.class),
                                              "key", "secret", "url", "url", "scope", RESOURCE_OWNER_ID, null, null);

    when(initialContext.getAccessToken()).thenReturn(ACCESS_TOKEN);
    when(initialContext.getRefreshToken()).thenReturn(REFRESH_TOKEN);
    when(initialContext.getResourceOwnerId()).thenReturn(RESOURCE_OWNER_ID);

    when(refreshedContext.getAccessToken()).thenReturn(NEW_TOKEN);
    when(refreshedContext.getRefreshToken()).thenReturn(NEW_REFRESH_TOKEN);
    when(refreshedContext.getResourceOwnerId()).thenReturn(RESOURCE_OWNER_ID);
  }

  @Test
  public void onRefreshToken() {
    ArgumentCaptor<AuthorizationCodeListener> listenerCaptor = forClass(AuthorizationCodeListener.class);
    Reference<ResourceOwnerOAuthContext> newContext = new Reference<>();

    UpdatingAuthorizationCodeState state = new UpdatingAuthorizationCodeState(oAuthConfig,
                                                                              dancer,
                                                                              initialContext,
                                                                              newContext::set);

    verify(dancer).addListener(anyString(), listenerCaptor.capture());

    assertThat(state.getAccessToken(), equalTo(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));

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
                                                                              newContext::set);

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
                                                                              newContext::set);

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
                                                                              newContext::set);

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
}
