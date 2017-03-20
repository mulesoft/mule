/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.service.http.api.HttpConstants.Method.GET;
import static org.mule.service.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeQueryString;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeString;
import static org.mule.services.oauth.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.STATE_PARAMETER;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.test.oauth2.asserter.AuthorizationRequestAsserter;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunnerDelegateTo(Parameterized.class)
public class AuthorizationCodeFullConfigTestCase extends AbstractOAuthAuthorizationTestCase {

  public final String CUSTOM_RESPONSE_PARAMETER1_VALUE = "token-resp-value1";
  public final String CUSTOM_RESPONSE_PARAMETER2_VALUE = "token-resp-value2";

  @Rule
  public SystemProperty localAuthorizationUrl =
      new SystemProperty("local.authorization.url",
                         format("%s://localhost:%d/authorization", getProtocol(), localHostPort.getNumber()));
  @Rule
  public SystemProperty authorizationUrl =
      new SystemProperty("authorization.url",
                         format("%s://localhost:%d" + AUTHORIZE_PATH, getProtocol(), oauthHttpsServerPort.getNumber()));
  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url",
                         format("%s://localhost:%d" + TOKEN_PATH, getProtocol(), oauthHttpsServerPort.getNumber()));
  @Rule
  public SystemProperty authenticationRequestParam1 = new SystemProperty("auth.request.param1", "auth-req-param1");
  @Rule
  public SystemProperty authenticationRequestParam2 = new SystemProperty("auth.request.param2", "auth-req-param2");
  @Rule
  public SystemProperty authenticationRequestValue1 = new SystemProperty("auth.request.value1", "auth-req-value1");
  @Rule
  public SystemProperty authenticationRequestValue2 = new SystemProperty("auth.request.value2", "auth-req-value2");
  @Rule
  public SystemProperty customTokenResponseParameter1Name = new SystemProperty("custom.param.extractor1", "token-resp-param1");
  @Rule
  public SystemProperty customTokenResponseParameter2Name = new SystemProperty("custom.param.extractor2", "token-resp-param2");

  private String[] configFiles;

  @Rule
  public TestHttpClient httpClient =
      new TestHttpClient.Builder(getService(HttpService.class)).tlsContextFactory(() -> createClientTlsContextFactory()).build();

  @Override
  protected String[] getConfigFiles() {
    return configFiles;
  }

  public AuthorizationCodeFullConfigTestCase(String[] configFiles) {
    this.configFiles = configFiles;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    final String operationsConfig = "operations/operations-config.xml";
    return Arrays.asList(new Object[][] {
        new String[] {"authorization-code/authorization-code-full-config-tls-global.xml", operationsConfig}},
                         new Object[][] {new String[] {"authorization-code/authorization-code-full-config-tls-nested.xml",
                             operationsConfig}});
  }

  @Test
  public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception {
    wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(OK.getStatusCode())));

    HttpRequest request = HttpRequest.builder().setUri(localAuthorizationUrl.getValue()).setMethod(GET).build();
    httpClient.send(request, RECEIVE_TIMEOUT, true, null);

    final List<LoggedRequest> requests = findAll(getRequestedFor(urlMatching(AUTHORIZE_PATH + ".*")));
    assertThat(requests, hasSize(1));

    AuthorizationRequestAsserter.create((requests.get(0))).assertMethodIsGet().assertClientIdIs(clientId.getValue())
        .assertRedirectUriIs(localCallbackUrl.getValue()).assertScopeIs(scopes.getValue()).assertStateIs(state.getValue())
        .assertContainsCustomParameter(authenticationRequestParam1.getValue(), authenticationRequestValue1.getValue())
        .assertContainsCustomParameter(authenticationRequestParam2.getValue(), authenticationRequestValue2.getValue())
        .assertResponseTypeIsCode();
  }

  @Test
  public void hitRedirectUrlAndGetToken() throws Exception {

    final ImmutableMap<Object, Object> tokenUrlResponseParameters =
        ImmutableMap.builder().put(ACCESS_TOKEN_PARAMETER, ACCESS_TOKEN)
            .put(EXPIRES_IN_PARAMETER, EXPIRES_IN).put(REFRESH_TOKEN_PARAMETER, REFRESH_TOKEN)
            .put(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE)
            .put(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE).build();


    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse()
        .withHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())
        .withBody(encodeString(tokenUrlResponseParameters, UTF_8))));

    final ImmutableMap<String, String> redirectUrlQueryParams = ImmutableMap.<String, String>builder()
        .put(CODE_PARAMETER, AUTHENTICATION_CODE).put(STATE_PARAMETER, state.getValue()).build();

    HttpRequest request = HttpRequest.builder()
        .setUri(localCallbackUrl.getValue() + "?" + encodeQueryString(redirectUrlQueryParams)).setMethod(GET).build();
    httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    verifyRequestDoneToTokenUrlForAuthorizationCode();

    verifyTokenManagerAccessToken();
    verifyTokenManagerRefreshToken();
    verifyTokenManagerExpiresIn();
    verifyTokenManagerState();
    verifyTokenManagerCustomParameterExtractor(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE);
    verifyTokenManagerCustomParameterExtractor(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE);
  }

  private TlsContextFactory createClientTlsContextFactory() {
    try {
      DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
      tlsContextFactory.setTrustStorePath("ssltest-cacerts.jks");
      tlsContextFactory.setTrustStorePassword("changeit");
      tlsContextFactory.setKeyStorePath("ssltest-keystore.jks");
      tlsContextFactory.setKeyStorePassword("changeit");
      tlsContextFactory.setKeyManagerPassword("changeit");

      return tlsContextFactory;
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  protected String getProtocol() {
    return HTTPS.getScheme();
  }
}
