/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.mule.extension.oauth2.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_CLIENT_CREDENTIALS;
import static org.mule.extension.oauth2.internal.OAuthConstants.GRANT_TYPE_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.REDIRECT_URI_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.SCOPE_PARAMETER;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeString;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.service.http.api.HttpHeaders;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Rule;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;

@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-sockets", "org.mule.modules:mule-module-http-ext"},
    providedInclusions = "org.mule.modules:mule-module-sockets")
public abstract class AbstractOAuthAuthorizationTestCase extends MuleArtifactFunctionalTestCase {

  public static final int REQUEST_TIMEOUT = 5000;

  public static final String TOKEN_PATH = "/token";
  public static final String AUTHENTICATION_CODE = "9WGJOBZXAvSibONGAxVlLuML0e0RhfX4";
  public static final String ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4gu6vpCobc2ya";
  public static final String REFRESH_TOKEN = "cry825cyCs2O0j7tRXXVS4AXNu7hsO5wbWjcBoFFcJePy5zZwuQEevIp6hsUaywp";
  public static final String EXPIRES_IN = "3897";
  public static final String AUTHORIZE_PATH = "/authorize";
  protected final DynamicPort oauthServerPort = new DynamicPort("port2");
  protected final DynamicPort oauthHttpsServerPort = new DynamicPort("port3");
  private String keyStorePath = Thread.currentThread().getContextClassLoader().getResource("ssltest-keystore.jks").getPath();
  private String keyStorePassword = "changeit";

  @Rule
  public final DynamicPort localHostPort = new DynamicPort("localHostPort");

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(oauthServerPort.getNumber())
      .httpsPort(oauthHttpsServerPort.getNumber()).keystorePath(keyStorePath).keystorePassword(keyStorePassword));

  @Rule
  public SystemProperty clientId = new SystemProperty("client.id", "ndli93xdws2qoe6ms1d389vl6bxquv3e");
  @Rule
  public SystemProperty clientSecret = new SystemProperty("client.secret", "yL692Az1cNhfk1VhTzyx4jOjjMKBrO9T");
  @Rule
  public SystemProperty scopes = new SystemProperty("scopes", "expected scope");
  @Rule
  public SystemProperty state = new SystemProperty("state", "expected state");
  @Rule
  public SystemProperty oauthServerPortNumber =
      new SystemProperty("oauth.server.port", String.valueOf(oauthServerPort.getNumber()));
  @Rule
  public SystemProperty localCallbackPath =
      new SystemProperty("local.callback.path", "/callback");
  @Rule
  public SystemProperty localCallbackUrl =
      new SystemProperty("local.callback.url",
                         format("%s://localhost:%d%s", getProtocol(), localHostPort.getNumber(), localCallbackPath.getValue()));
  @Rule
  public SystemProperty wireMockHttpPort = new SystemProperty("oauthServerHttpPort", String.valueOf(oauthServerPort.getNumber()));

  @Before
  public void before() throws Exception {
    try {
      // Force the initialization of the OAuth context
      // TODO MULE-11405 switch to the client
      // muleContext.getRegistry().lookupObject(ExtensionsClient.class).execute("HTTP", "request",
      // builder().configName("requestConfig").build());
      flowRunner("testFlow").runNoVerify();
    } catch (Exception e) {
      // Ignore
    }
  }

  protected String getProtocol() {
    return "http";
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType() {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(ACCESS_TOKEN);
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(String accessToken) {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(accessToken, REFRESH_TOKEN);
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(String accessToken, String refreshToken) {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody("{" + "\""
        + ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," + "\"" + EXPIRES_IN_PARAMETER
        + "\":" + EXPIRES_IN + "," + "\"" + REFRESH_TOKEN_PARAMETER + "\":\"" + refreshToken + "\"}");
  }

  protected void configureWireMockToExpectOfflineTokenPathRequestForAuthorizationCodeGrantType(String accessToken) {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody("{" + "\""
        + ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," + "\"" + EXPIRES_IN_PARAMETER
        + "\":" + EXPIRES_IN + "," + "\"}");
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody(String body) {
    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withBody(body)));
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeAndFail() {
    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.getStatusCode())));
  }

  protected void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType() {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType(ACCESS_TOKEN);
  }

  protected void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(ImmutableMap customParameters) {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(ACCESS_TOKEN, customParameters);
  }

  protected void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType(String accessToken) {
    wireMockRule
        .stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withBody("{" + "\"" + ACCESS_TOKEN_PARAMETER
            + "\":\"" + accessToken + "\"," + "\"" + EXPIRES_IN_PARAMETER + "\":\"" + EXPIRES_IN + "\"}")));
  }


  protected void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(String accessToken) {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(accessToken,
                                                                                          new ImmutableMap.Builder().build());
  }

  private void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(String accessToken,
                                                                                                     ImmutableMap customParameters) {
    customParameters = new ImmutableMap.Builder().putAll(customParameters).put(ACCESS_TOKEN_PARAMETER, accessToken)
        .put(EXPIRES_IN_PARAMETER, EXPIRES_IN).build();
    final ImmutableMap.Builder bodyParametersMapBuilder = new ImmutableMap.Builder();
    for (Object customParameterName : customParameters.keySet()) {
      bodyParametersMapBuilder.put(customParameterName, customParameters.get(customParameterName));
    }
    final String body = encodeString(UTF_8, bodyParametersMapBuilder.build());
    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withBody(body)
        .withHeader(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())));
  }

  protected void verifyRequestDoneToTokenUrlForAuthorizationCode() throws UnsupportedEncodingException {
    wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
        .withRequestBody(containing(CLIENT_ID_PARAMETER + "=" + encode(clientId.getValue(), UTF_8.name())))
        .withRequestBody(containing(CODE_PARAMETER + "=" + encode(AUTHENTICATION_CODE, UTF_8.name())))
        .withRequestBody(containing(CLIENT_SECRET_PARAMETER + "=" + encode(clientSecret.getValue(), UTF_8.name())))
        .withRequestBody(containing(GRANT_TYPE_PARAMETER + "=" + encode(GRANT_TYPE_AUTHENTICATION_CODE, UTF_8.name())))
        .withRequestBody(containing(REDIRECT_URI_PARAMETER + "=" + encode(localCallbackUrl.getValue(), UTF_8.name()))));
  }

  protected void verifyRequestDoneToTokenUrlForClientCredentials() throws UnsupportedEncodingException {
    verifyRequestDoneToTokenUrlForClientCredentials(null);
  }

  protected void verifyRequestDoneToTokenUrlForClientCredentials(String scope) throws UnsupportedEncodingException {
    verifyRequestDoneToTokenUrlForClientCredentials(scope, false);
  }

  protected void verifyRequestDoneToTokenUrlForClientCredentials(String scope, boolean encodeInBody)
      throws UnsupportedEncodingException {
    final RequestPatternBuilder verification =
        postRequestedFor(urlEqualTo(TOKEN_PATH))
            .withRequestBody(containing(GRANT_TYPE_PARAMETER + "=" + encode(GRANT_TYPE_CLIENT_CREDENTIALS, UTF_8.name())));
    if (encodeInBody == true) {
      verification
          .withRequestBody(containing(CLIENT_ID_PARAMETER + "=" + encode(clientId.getValue(), UTF_8.name())))
          .withRequestBody(containing(CLIENT_SECRET_PARAMETER + "=" + encode(clientSecret.getValue(), UTF_8.name())));
    } else {
      verification.withHeader(AUTHORIZATION, containing("Basic "
          + encodeBase64String(format("%s:%s", clientId.getValue(), clientSecret.getValue()).getBytes())));
    }
    if (scope != null) {
      verification.withRequestBody(containing(SCOPE_PARAMETER + "=" + encode(scope, UTF_8.name())));
    }
    wireMockRule.verify(verification);
  }
}
