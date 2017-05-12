/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;
import org.mule.test.oauth.TestOAuthExtension;

import com.github.tomakehurst.wiremock.client.WireMock;

import org.junit.Test;

public class OAuthExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"dynamic-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Test
  public void authorizeAndStartDancingBaby() throws Exception {
    simulateDanceStart();

    verifyAuthUrlRequest();
  }

  protected void verifyAuthUrlRequest() {
    wireMock.verify(getRequestedFor(urlPathEqualTo("/" + AUTHORIZE_PATH))
        .withQueryParam("redirect_uri", equalTo((toUrl(CALLBACK_PATH, callbackPort.getNumber()))))
        .withQueryParam("client_id", equalTo(CONSUMER_KEY))
        .withQueryParam("scope", equalTo(SCOPES.replaceAll(" ", "\\+")))
        .withQueryParam("state", containing(STATE)));
  }

  @Test
  public void receiveAccessTokenAndUserConnection() throws Exception {
    simulateCallback();

    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner("getConnection")
        .withVariable(OWNER_ID_VARIABLE_NAME, OWNER_ID)
        .run().getMessage().getPayload().getValue()).getState();

    assertThat(connection, is(notNullValue()));
    assertThat(connection.getApiVersion(), is(34.0D));
    assertThat(connection.getDisplay(), is("PAGE"));
    assertThat(connection.isPrompt(), is(false));
    assertThat(connection.isImmediate(), is(true));
    assertThat(connection.getInstanceId(), is(INSTANCE_ID));
    assertThat(connection.getUserId(), is(USER_ID));

    AuthorizationCodeState state = connection.getState();
    assertThat(state.getAccessToken(), is(ACCESS_TOKEN));
    assertThat(state.getExpiresIn().get(), is(EXPIRES_IN));
    assertThat(state.getRefreshToken().get(), is(REFRESH_TOKEN));
    assertThat(state.getState().get(), is(STATE));
    assertThat(state.getResourceOwnerId(), is(OWNER_ID));

    assertOAuthStateStored(DEFAULT_USER_OBJECT_STORE_NAME, OWNER_ID);
  }

  @Test
  public void refreshToken() throws Exception {
    receiveAccessTokenAndUserConnection();
    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));
    flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, OWNER_ID).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void callbackFlows() throws Exception {
    authorizeAndStartDancingBaby();
    receiveAccessTokenAndUserConnection();

    TestOAuthExtension config = getConfigurationFromRegistry("oauth", Event.builder(getInitialiserEvent())
        .addVariable(OWNER_ID_VARIABLE_NAME, OWNER_ID)
        .build(), muleContext);

    check(REQUEST_TIMEOUT, 500, () -> {
      assertThat(config.getCapturedAuthCodeRequests(), hasSize(1));
      assertThat(config.getCapturedAuthCodeStates(), hasSize(1));
      return true;
    });

    assertBeforeCallbackPayload(config);
    assertAfterCallbackPayload(config);
  }

  @Test
  public void unauthorize() throws Exception {
    authorizeAndStartDancingBaby();
    receiveAccessTokenAndUserConnection();

    flowRunner("unauthorize").withVariable(OWNER_ID_VARIABLE_NAME, OWNER_ID).run();
    ObjectStore objectStore = getObjectStore(DEFAULT_USER_OBJECT_STORE_NAME);
    assertThat(objectStore.contains(OWNER_ID), is(false));
  }

  protected void assertBeforeCallbackPayload(TestOAuthExtension config) {
    AuthCodeRequest request = config.getCapturedAuthCodeRequests().get(0);
    assertThat(request.getResourceOwnerId(), is(OWNER_ID));
    assertScopes(request);
    assertThat(request.getState().get(), is(STATE));
  }

  protected void assertScopes(AuthCodeRequest request) {
    assertThat(request.getScopes().get(), is(SCOPES));
  }

  private void assertAfterCallbackPayload(TestOAuthExtension config) {
    AuthorizationCodeState state = config.getCapturedAuthCodeStates().get(0);
    assertThat(state.getAccessToken(), is(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), is(REFRESH_TOKEN));
    assertThat(state.getResourceOwnerId(), is(OWNER_ID));
    assertThat(state.getExpiresIn().get(), is(EXPIRES_IN));
    assertThat(state.getState().get(), is(STATE));
    assertThat(state.getAuthorizationUrl(), is(authUrl));
    assertThat(state.getAccessTokenUrl(), is(tokenUrl));
    assertThat(state.getConsumerKey(), is(CONSUMER_KEY));
    assertThat(state.getConsumerSecret(), is(CONSUMER_SECRET));
  }
}
