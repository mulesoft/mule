/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.mule.module.http.HttpParser;
import org.mule.module.http.ParameterMap;
import org.mule.module.oauth2.asserter.OAuthStateFunctionAsserter;
import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.module.oauth2.internal.state.UserOAuthState;
import org.mule.tck.junit4.rule.SystemProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AuthorizationCodeCustomTokenRequestTestCase extends AbstractAuthorizationCodeFunctionalTestCase
{

    private final String REDIRECT_PATH = "redirect";

    private final String configFile;
    private final String oauthStateId;

    @Rule
    public SystemProperty redirectUrl = new SystemProperty("redirect.url", String.format("http://localhost:%d/%s", localHostPort.getNumber(), REDIRECT_PATH));
    @Rule
    public SystemProperty redirectUrlHost = new SystemProperty("redirect.url.host", String.format("localhost"));
    @Rule
    public SystemProperty redirectUrlPort = new SystemProperty("redirect.url.port", String.valueOf(localHostPort.getNumber()));
    @Rule
    public SystemProperty redirectUrlPath = new SystemProperty("redirect.url.path", REDIRECT_PATH);
    @Rule
    public SystemProperty tokenHost = new SystemProperty("token.host", String.format("localhost"));
    @Rule
    public SystemProperty tokenPort = new SystemProperty("token.port", String.valueOf(oauthServerPort.getNumber()));
    @Rule
    public SystemProperty tokenPath = new SystemProperty("token.path", TOKEN_PATH);

    public AuthorizationCodeCustomTokenRequestTestCase(String configFile, String oauthStateId)
    {
        this.configFile = configFile;
        this.oauthStateId = oauthStateId;
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]
                                     {
                                             {"authorization-code-custom-token-request-config.xml", UserOAuthState.DEFAULT_USER_ID},
                                             {"authorization-code-custom-token-request-with-own-uri-config.xml", "peter"},
                                     });
    }


    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void hitRedirectUrlAndGetToken() throws Exception
    {
        configureWireMockToExpectTokenPathRequestAndReturnJsonResponse();


        final ParameterMap queryParams = new ParameterMap()
                .putAndReturn(OAuthConstants.CODE_PARAMETER, AUTHENTICATION_CODE)
                .putAndReturn(OAuthConstants.STATE_PARAMETER, oauthStateId);

        Request.Get(redirectUrl.getValue() + "?" + HttpParser.encodeQueryString(queryParams)).socketTimeout(REQUEST_TIMEOUT).execute();

        wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_ID_PARAMETER + "=" + URLEncoder.encode(clientId.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CODE_PARAMETER + "=" + URLEncoder.encode(AUTHENTICATION_CODE, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_SECRET_PARAMETER + "=" + URLEncoder.encode(clientSecret.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.GRANT_TYPE_PARAMETER + "=" + URLEncoder.encode(OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.REDIRECT_URI_PARAMETER + "=" + URLEncoder.encode(redirectUrl.getValue(), StandardCharsets.UTF_8.name()))));

        OAuthStateFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), "customConfig", oauthStateId)
                .assertAccessTokenIs(ACCESS_TOKEN)
                .assertRefreshTokenIs(REFRESH_TOKEN);
    }
}
