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
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.oauth2.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.extension.oauth2.internal.OAuthConstants.STATE_PARAMETER;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.service.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeQueryString;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeString;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.test.oauth2.asserter.AuthorizationRequestAsserter;
import org.mule.test.oauth2.asserter.OAuthContextFunctionAsserter;
import org.mule.test.runner.RunnerDelegateTo;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

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

  private String configFile;

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  public AuthorizationCodeFullConfigTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[] {"authorization-code/authorization-code-full-config-tls-global.xml"},
                         new Object[] {"authorization-code/authorization-code-full-config-tls-nested.xml"});
  }

  @Test
  public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception {
    wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(200)));

    HttpRequestOptions options = newOptions().enableFollowsRedirect().tlsContextFactory(createClientTlsContextFactory()).build();

    muleContext.getClient().send(localAuthorizationUrl.getValue(), InternalMessage.builder().nullPayload().build(), options);

    final List<LoggedRequest> requests = findAll(getRequestedFor(urlMatching(AUTHORIZE_PATH + ".*")));
    assertThat(requests.size(), is(1));

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

    muleContext.getClient().send(localCallbackUrl.getValue() + "?" + encodeQueryString(redirectUrlQueryParams),
                                 InternalMessage.builder().nullPayload().build(),
                                 newOptions().tlsContextFactory(createClientTlsContextFactory()).build());

    verifyRequestDoneToTokenUrlForAuthorizationCode();

    OAuthContextFunctionAsserter.createFrom(muleContext.getRegistry().get("tokenManagerConfig"))
        .assertAccessTokenIs(ACCESS_TOKEN).assertExpiresInIs(EXPIRES_IN).assertRefreshTokenIs(REFRESH_TOKEN)
        .assertState(state.getValue())
        .assertContainsCustomTokenResponseParam(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE)
        .assertContainsCustomTokenResponseParam(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE);
  }

  private TlsContextFactory createClientTlsContextFactory() throws IOException {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    tlsContextFactory.setTrustStorePath("ssltest-cacerts.jks");
    tlsContextFactory.setTrustStorePassword("changeit");
    tlsContextFactory.setKeyStorePath("ssltest-keystore.jks");
    tlsContextFactory.setKeyStorePassword("changeit");
    tlsContextFactory.setKeyManagerPassword("changeit");

    return tlsContextFactory;
  }

  @Override
  protected String getProtocol() {
    return HTTPS.getScheme();
  }
}
