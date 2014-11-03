/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.module.http.HttpParser;
import org.mule.module.http.ParameterMap;
import org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.module.oauth2.asserter.AuthorizationRequestAsserter;
import org.mule.module.oauth2.asserter.OAuthContextFunctionAsserter;
import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.tck.junit4.rule.SystemProperty;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeFullConfigTestCase extends AbstractOAuthAuthorizationTestCase
{

    public final String CUSTOM_RESPONSE_PARAMETER1_VALUE = "token-resp-value1";
    public final String CUSTOM_RESPONSE_PARAMETER2_VALUE = "token-resp-value2";

    @Rule
    public SystemProperty localAuthorizationUrl = new SystemProperty("local.authorization.url", String.format("http://localhost:%d/authorization", localHostPort.getNumber()));
    @Rule
    public SystemProperty authorizationUrl = new SystemProperty("authorization.url", String.format("http://localhost:%d" + AUTHORIZE_PATH, oauthServerPort.getNumber()));
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));
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


    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-full-config.xml";
    }

    @Test
    public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception
    {
        wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(200)));

        Request.Get(localAuthorizationUrl.getValue())
                .connectTimeout(REQUEST_TIMEOUT)
                .socketTimeout(REQUEST_TIMEOUT)
                .execute();

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
        final ParameterMap tokenUrlResponseParameters = new ParameterMap()
                .putAndReturn(OAuthConstants.ACCESS_TOKEN_PARAMETER, ACCESS_TOKEN)
                .putAndReturn(OAuthConstants.EXPIRES_IN_PARAMETER, EXPIRES_IN)
                .putAndReturn(OAuthConstants.REFRESH_TOKEN_PARAMETER, REFRESH_TOKEN)
                .putAndReturn(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE)
                .putAndReturn(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE);


        wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH))
                                     .willReturn(aResponse()
                                                         .withHeader(HttpHeaders.CONTENT_TYPE, org.mule.module.http.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)
                                                         .withBody(HttpParser.encodeString("UTF-8", tokenUrlResponseParameters))));

        final ParameterMap redirectUrlQueryParams = new ParameterMap()
                .putAndReturn(OAuthConstants.CODE_PARAMETER, AUTHENTICATION_CODE)
                .putAndReturn(OAuthConstants.STATE_PARAMETER, state.getValue());

        Request.Get(redirectUrl.getValue() + "?" + HttpParser.encodeQueryString(redirectUrlQueryParams))
                .connectTimeout(REQUEST_TIMEOUT)
                .socketTimeout(REQUEST_TIMEOUT)
                .execute();

        verifyRequestDoneToTokenUrlForAuthorizationCode();

        OAuthContextFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), "fullConfig")
                .assertAccessTokenIs(ACCESS_TOKEN)
                .assertExpiresInIs(EXPIRES_IN)
                .assertRefreshTokenIs(REFRESH_TOKEN)
                .assertState(state.getValue())
                .assertContainsCustomTokenResponseParam(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE)
                .assertContainsCustomTokenResponseParam(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE);
    }

}
