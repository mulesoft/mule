/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.OAuthCallbackConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.UpdatingAuthorizationCodeState;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.builder.AuthorizationCodeListener;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class UpdatingAuthorizationCodeStateTestCase extends AbstractMuleTestCase {

  private static final String ACCESS_TOKEN = "myToken";
  private static final String REFRESH_TOKEN = "myRefreshToken";
  private static final String NEW_TOKEN = "newToken";
  private static final String NEW_REFRESH_TOKEN = "newRefresh";

  private OAuthConfig oAuthConfig;

  @Mock
  private AuthorizationCodeOAuthDancer dancer;

  @Mock
  private ResourceOwnerOAuthContext initialContext;

  @Mock
  private ResourceOwnerOAuthContext refreshedContext;

  @Before
  public void before() {
    oAuthConfig = new OAuthConfig("configName",
                                  new AuthorizationCodeConfig("key", "secret", "url", "url", "scope", "id", null, null),
                                  mock(OAuthCallbackConfig.class),
                                  empty(),
                                  new AuthorizationCodeGrantType("url", "url", "#[s]", "reg", "#[x]", "sd"),
                                  emptyMap(),
                                  emptyMap());

    when(initialContext.getAccessToken()).thenReturn(ACCESS_TOKEN);
    when(initialContext.getRefreshToken()).thenReturn(REFRESH_TOKEN);

    when(refreshedContext.getAccessToken()).thenReturn(NEW_TOKEN);
    when(refreshedContext.getRefreshToken()).thenReturn(NEW_REFRESH_TOKEN);
  }

  @Test
  public void onRefreshToken() throws Exception {
    ArgumentCaptor<AuthorizationCodeListener> listenerCaptor = forClass(AuthorizationCodeListener.class);
    Reference<ResourceOwnerOAuthContext> newContext = new Reference<>();

    UpdatingAuthorizationCodeState state = new UpdatingAuthorizationCodeState(oAuthConfig,
                                                                              dancer,
                                                                              initialContext,
                                                                              newContext::set);

    verify(dancer).addListener(listenerCaptor.capture());

    assertThat(state.getAccessToken(), equalTo(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));

    AuthorizationCodeListener listener = listenerCaptor.getValue();
    listener.onTokenRefreshed(refreshedContext);

    assertThat(state.getAccessToken(), equalTo(NEW_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(NEW_REFRESH_TOKEN));

    assertThat(newContext.get(), is(sameInstance(refreshedContext)));
  }

  @Test
  public void onReAuthorization() throws Exception {
    ArgumentCaptor<AuthorizationCodeListener> listenerCaptor = forClass(AuthorizationCodeListener.class);
    Reference<ResourceOwnerOAuthContext> newContext = new Reference<>();

    UpdatingAuthorizationCodeState state = new UpdatingAuthorizationCodeState(oAuthConfig,
                                                                              dancer,
                                                                              initialContext,
                                                                              newContext::set);

    verify(dancer).addListener(listenerCaptor.capture());

    assertThat(state.getAccessToken(), equalTo(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(REFRESH_TOKEN));

    AuthorizationCodeListener listener = listenerCaptor.getValue();
    listener.onAuthorizationCompleted(refreshedContext);

    assertThat(state.getAccessToken(), equalTo(NEW_TOKEN));
    assertThat(state.getRefreshToken().get(), equalTo(NEW_REFRESH_TOKEN));

    assertThat(newContext.get(), is(sameInstance(refreshedContext)));
  }
}
