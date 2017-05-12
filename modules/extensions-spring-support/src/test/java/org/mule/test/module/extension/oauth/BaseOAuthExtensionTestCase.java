/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.apache.http.client.fluent.Request.Get;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeQueryString;
import static org.mule.services.oauth.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.tck.probe.PollingProber.check;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Map;

import org.apache.http.client.fluent.Response;
import org.junit.Rule;

public abstract class BaseOAuthExtensionTestCase extends AbstractExtensionFunctionalTestCase {

  protected static final int REQUEST_TIMEOUT = 10000;
  protected static final String LOCAL_AUTH_PATH = "dance";
  protected static final String CALLBACK_PATH = "callback";
  protected static final String OWNER_ID_VARIABLE_NAME = "ownerId";
  protected static final String OWNER_ID = "MG";
  protected static final String TOKEN_PATH = "token";
  protected static final String STATE = "myState";
  protected static final String AUTHORIZE_PATH = "authorize";
  protected static final String USER_ID = "35";
  protected static final String INSTANCE_ID = "staging";
  protected static final String SCOPES = "this, that, those";
  protected static final String CONSUMER_KEY = "ndli93xdws2qoe6ms1d389vl6bxquv3e";
  protected static final String CONSUMER_SECRET = "yL692Az1cNhfk1VhTzyx4jOjjMKBrO9T";
  protected static final String ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4gu6vpCobc2ya";
  protected static final String REFRESH_TOKEN = "cry825cyCs2O0j7tRXXVS4AXNu7hsO5wbWjcBoFFcJePy5zZwuQEevIp6hsUaywp";
  protected static final String EXPIRES_IN = "3897";
  protected static final String STATE_PARAMETER = "state";
  protected static final String CODE_PARAMETER = "code";

  @Rule
  public SystemProperty consumerKey = new SystemProperty("consumerKey", CONSUMER_KEY);

  @Rule
  public SystemProperty consumerSecret = new SystemProperty("consumerSecret", CONSUMER_SECRET);

  @Rule
  public SystemProperty localAuthPath = new SystemProperty("localAuthPath", LOCAL_AUTH_PATH);

  @Rule
  public SystemProperty scope = new SystemProperty("scopes", SCOPES);

  @Rule
  public DynamicPort callbackPort = new DynamicPort("callbackPort");

  @Rule
  public SystemProperty callbackPath = new SystemProperty("callbackPath", CALLBACK_PATH);

  @Rule
  public DynamicPort oauthServerPort = new DynamicPort("oauthServerPort");

  @Rule
  public SystemProperty oauthProvider = new SystemProperty("callbackPath", CALLBACK_PATH);

  @Rule
  public WireMockRule wireMock = new WireMockRule(wireMockConfig().port(oauthServerPort.getNumber()));

  protected String authUrl = toUrl(AUTHORIZE_PATH, oauthServerPort.getNumber());

  @Rule
  public SystemProperty authorizationUrl = new SystemProperty("authorizationUrl", authUrl);


  protected String tokenUrl = toUrl(TOKEN_PATH, oauthServerPort.getNumber());
  @Rule
  public SystemProperty accessTokenUrl = new SystemProperty("accessTokenUrl", tokenUrl);


  protected String toUrl(String path, int port) {
    return format("http://localhost:%d/%s", port, path);
  }

  protected void assertOAuthStateStored(String objectStoreName, String resourceOwnerId) throws Exception {
    ListableObjectStore objectStore = getObjectStore(objectStoreName);

    ResourceOwnerOAuthContext context = (ResourceOwnerOAuthContext) objectStore.retrieve(resourceOwnerId);
    assertThat(context.getResourceOwnerId(), is(resourceOwnerId));
  }

  protected ListableObjectStore getObjectStore(String objectStoreName) {
    ListableObjectStore objectStore = muleContext.getObjectStoreManager().getObjectStore(objectStoreName);
    assertThat(objectStore, is(notNullValue()));
    return objectStore;
  }

  protected void simulateDanceStart() throws IOException {
    wireMock.stubFor(get(urlMatching("/" + LOCAL_AUTH_PATH)).willReturn(aResponse().withStatus(OK.getStatusCode())));
    Map<String, String> queryParams = ImmutableMap.<String, String>builder()
        .put("resourceOwnerId", OWNER_ID)
        .put("state", STATE)
        .build();

    String localAuthUrl = toUrl(LOCAL_AUTH_PATH, callbackPort.getNumber());
    Get(localAuthUrl + "?" + encodeQueryString(queryParams))
        .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();
  }

  protected void simulateCallback() {
    final String authCode = "chu chu ua, chu chu ua";

    Map<String, String> queryParams = ImmutableMap.<String, String>builder()
        .put(STATE_PARAMETER, String.format("%s:resourceOwnerId=%s", STATE, OWNER_ID))
        .put(CODE_PARAMETER, authCode)
        .build();

    stubTokenUrl(accessTokenContent());

    check(REQUEST_TIMEOUT, 500, () -> {
      Response response = Get(toUrl(CALLBACK_PATH, callbackPort.getNumber()) + "?" + encodeQueryString(queryParams))
          .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();

      assertThat(response.returnResponse().getStatusLine().getStatusCode(), is(OK.getStatusCode()));
      return true;
    });
  }

  protected void stubTokenUrl(String responseContent) {
    wireMock.stubFor(post(urlMatching("/" + TOKEN_PATH)).willReturn(aResponse()
        .withStatus(OK.getStatusCode())
        .withBody(responseContent)
        .withHeader(CONTENT_TYPE, "application/json")));
  }

  protected String accessTokenContent() {
    return accessTokenContent(ACCESS_TOKEN);
  }

  protected String accessTokenContent(String accessToken) {
    return "{" +
        "\"" + ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," +
        "\"" + EXPIRES_IN_PARAMETER + "\":" + EXPIRES_IN + "," +
        "\"" + REFRESH_TOKEN_PARAMETER + "\":\"" + REFRESH_TOKEN + "\"," +
        "\"" + "id" + "\":\"" + USER_ID + "\"," +
        "\"" + "instance_url" + "\":\"" + INSTANCE_ID + "\"" +
        "}";
  }
}
