/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.http.HttpHeaders;
import org.mule.module.oauth2.asserter.OAuthStateFunctionAsserter;
import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.module.oauth2.internal.state.OAuthStateRegistry;
import org.mule.module.oauth2.internal.state.UserOAuthState;
import org.mule.tck.junit4.rule.SystemProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AuthorizationCodeRefreshTokenConfigTestCase extends AbstractAuthorizationCodeFunctionalTestCase
{

    private static final String RESOURCE_PATH = "/resource";
    public static final String RESOURCE_RESULT = "resource result";
    public static final String REFRESHED_ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4guasdfsdfa";
    public static final String MULTITENANT_OAUTH_CONFIG = "multitenantOauthConfig";
    public static final String SINGLE_TENANT_OAUTH_CONFIG = "oauthConfig";

    public static final String USER_ID_JOHN = "john";
    public static final String JOHN_ACCESS_TOKEN = "123456789";
    public static final String USER_ID_TONY = "tony";
    public static final String TONY_ACCESS_TOKEN = "abcdefghi";

    @Rule
    public SystemProperty localAuthorizationUrl = new SystemProperty("local.authorization.url", String.format("http://localhost:%d/authorization", localHostPort.getNumber()));
    @Rule
    public SystemProperty authorizationUrl = new SystemProperty("authorization.url", String.format("http://localhost:%d" + AUTHORIZE_PATH, oauthServerPort.getNumber()));
    @Rule
    public SystemProperty redirectUrl = new SystemProperty("redirect.url", String.format("http://localhost:%d/redirect", localHostPort.getNumber()));
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));
    @Rule
    public SystemProperty tokenHost = new SystemProperty("token.host", String.format("localhost"));
    @Rule
    public SystemProperty tokenPort = new SystemProperty("token.port", String.valueOf(oauthServerPort.getNumber()));
    @Rule
    public SystemProperty tokenPath = new SystemProperty("token.path", TOKEN_PATH);
    @Rule
    public SystemProperty multitenantUser = new SystemProperty("multitenant.user", "john");

    private String configFile;


    public AuthorizationCodeRefreshTokenConfigTestCase(String config)
    {
        this.configFile = config;
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]
                                     {
                                             {"authorization-code-refresh-token-config.xml"},
                                             {"authorization-code-custom-token-request-refresh-token-config.xml"},
                                     });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void afterFailureDoRefreshTokenWithDefaultValueNoOauthStateId() throws Exception
    {
        executeRefreshToken("testFlow", SINGLE_TENANT_OAUTH_CONFIG, UserOAuthState.DEFAULT_USER_ID, 403);
    }

    @Test
    public void afterFailureDoRefreshTokenWithCustomValueWithOauthStateId() throws Exception
    {
        final OAuthStateRegistry contextOAuthState = muleContext.getRegistry().lookupObject(OAuthStateRegistry.class);
        contextOAuthState.getStateForConfig(MULTITENANT_OAUTH_CONFIG).getStateForUser(USER_ID_TONY).setAccessToken(TONY_ACCESS_TOKEN);
        contextOAuthState.getStateForConfig(MULTITENANT_OAUTH_CONFIG).getStateForUser(USER_ID_JOHN).setAccessToken(JOHN_ACCESS_TOKEN);

        executeRefreshToken("testMultitenantFlow", MULTITENANT_OAUTH_CONFIG, multitenantUser.getValue(), 500);

        OAuthStateFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), MULTITENANT_OAUTH_CONFIG, USER_ID_JOHN)
                .assertAccessTokenIs(REFRESHED_ACCESS_TOKEN)
                .assertState(null);
        OAuthStateFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), MULTITENANT_OAUTH_CONFIG, USER_ID_TONY)
                .assertAccessTokenIs(TONY_ACCESS_TOKEN)
                .assertState(null);
    }

    private void executeRefreshToken(String flowName, String oauthConfigName, String userId, int failureStatusCode) throws Exception
    {
        configureWireMockToExpectTokenPathRequestAndReturnJsonResponse(REFRESHED_ACCESS_TOKEN);

        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION,
                                                 containing(REFRESHED_ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withBody(RESOURCE_RESULT)));
        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION,
                                                 containing(ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withStatus(failureStatusCode)
                                                         .withBody("")));

        final OAuthStateRegistry oauthState = muleContext.getRegistry().lookupObject(OAuthStateRegistry.class);
        final UserOAuthState userOauthState = oauthState.getStateForConfig(oauthConfigName).getStateForUser(userId);
        userOauthState.setAccessToken(ACCESS_TOKEN);
        userOauthState.setRefreshToken(REFRESH_TOKEN);

        Flow flow = (Flow) getFlowConstruct(flowName);
        final MuleEvent testEvent = getTestEvent("message");
        testEvent.setFlowVariable("userId", userId);
        final MuleEvent result = flow.process(testEvent);
        assertThat(result.getMessage().getPayloadAsString(), is(RESOURCE_RESULT));

        wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_ID_PARAMETER + "=" + URLEncoder.encode(clientId.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.REFRESH_TOKEN_PARAMETER + "=" + URLEncoder.encode(REFRESH_TOKEN, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_SECRET_PARAMETER + "=" + URLEncoder.encode(clientSecret.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.GRANT_TYPE_PARAMETER + "=" + URLEncoder.encode(OAuthConstants.GRANT_TYPE_REFRESH_TOKEN, StandardCharsets.UTF_8.name()))));
    }
}
