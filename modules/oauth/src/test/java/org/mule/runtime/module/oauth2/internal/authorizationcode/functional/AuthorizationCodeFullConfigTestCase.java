/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.runtime.module.oauth2.asserter.AuthorizationRequestAsserter;
import org.mule.runtime.module.oauth2.asserter.OAuthContextFunctionAsserter;
import org.mule.runtime.module.oauth2.internal.OAuthConstants;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.tck.junit4.rule.SystemProperty;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

@RunWith(Parameterized.class)
public class AuthorizationCodeFullConfigTestCase extends AbstractOAuthAuthorizationTestCase
{

    public final String CUSTOM_RESPONSE_PARAMETER1_VALUE = "token-resp-value1";
    public final String CUSTOM_RESPONSE_PARAMETER2_VALUE = "token-resp-value2";

    @Rule
    public SystemProperty localAuthorizationUrl = new SystemProperty("local.authorization.url", String.format("%s://localhost:%d/authorization", getProtocol(), localHostPort.getNumber()));
    @Rule
    public SystemProperty authorizationUrl = new SystemProperty("authorization.url", String.format("%s://localhost:%d" + AUTHORIZE_PATH, getProtocol(), oauthHttpsServerPort.getNumber()));
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("%s://localhost:%d" + TOKEN_PATH, getProtocol(), oauthHttpsServerPort.getNumber()));
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
    protected String getConfigFile()
    {
        return configFile;
    }

    public AuthorizationCodeFullConfigTestCase(String configFile)
    {
        this.configFile = configFile;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[] {"authorization-code/authorization-code-full-config-tls-global.xml"},
                             new Object[] {"authorization-code/authorization-code-full-config-tls-nested.xml"});
    }

    @Test
    public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception
    {
        wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(200)));

        HttpRequestOptions options =  newOptions()
                .enableFollowsRedirect()
                .tlsContextFactory(createClientTlsContextFactory())
                .build();

        muleContext.getClient().send(localAuthorizationUrl.getValue(), getTestMuleMessage(NullPayload.getInstance()), options);

        final List<LoggedRequest> requests = findAll(getRequestedFor(urlMatching(AUTHORIZE_PATH + ".*")));
        assertThat(requests.size(), is(1));

        AuthorizationRequestAsserter.create((requests.get(0)))
                .assertMethodIsGet()
                .assertClientIdIs(clientId.getValue())
                .assertRedirectUriIs(redirectUrl.getValue())
                .assertScopeIs(scopes.getValue())
                .assertStateIs(state.getValue())
                .assertContainsCustomParameter(authenticationRequestParam1.getValue(), authenticationRequestValue1.getValue())
                .assertContainsCustomParameter(authenticationRequestParam2.getValue(), authenticationRequestValue2.getValue())
                .assertResponseTypeIsCode();
    }

    @Test
    public void hitRedirectUrlAndGetToken() throws Exception
    {

        final ImmutableMap<Object, Object> tokenUrlResponseParameters = ImmutableMap.builder()
                .put(OAuthConstants.ACCESS_TOKEN_PARAMETER, ACCESS_TOKEN)
                .put(OAuthConstants.EXPIRES_IN_PARAMETER, EXPIRES_IN)
                .put(OAuthConstants.REFRESH_TOKEN_PARAMETER, REFRESH_TOKEN)
                .put(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE)
                .put(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE).build();


        wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withHeader(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())
                                                                                .withBody(HttpParser.encodeString(UTF_8, tokenUrlResponseParameters))));

        final ImmutableMap<Object, Object> redirectUrlQueryParams = ImmutableMap.builder()
                .put(OAuthConstants.CODE_PARAMETER, AUTHENTICATION_CODE)
                .put(OAuthConstants.STATE_PARAMETER, state.getValue()).build();

        muleContext.getClient().send(redirectUrl.getValue() + "?" + HttpParser.encodeQueryString(redirectUrlQueryParams),
                getTestMuleMessage(NullPayload.getInstance()), newOptions().tlsContextFactory(createClientTlsContextFactory()).build());

        verifyRequestDoneToTokenUrlForAuthorizationCode();

        OAuthContextFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), "tokenManagerConfig")
                .assertAccessTokenIs(ACCESS_TOKEN)
                .assertExpiresInIs(EXPIRES_IN)
                .assertRefreshTokenIs(REFRESH_TOKEN)
                .assertState(state.getValue())
                .assertContainsCustomTokenResponseParam(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE)
                .assertContainsCustomTokenResponseParam(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE);
    }

    private TlsContextFactory createClientTlsContextFactory() throws IOException
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        tlsContextFactory.setTrustStorePath("ssltest-cacerts.jks");
        tlsContextFactory.setTrustStorePassword("changeit");
        tlsContextFactory.setKeyStorePath("ssltest-keystore.jks");
        tlsContextFactory.setKeyStorePassword("changeit");
        tlsContextFactory.setKeyManagerPassword("changeit");

        return tlsContextFactory;
    }

    @Override
    protected String getProtocol()
    {
        return HTTPS.getScheme();
    }
}
