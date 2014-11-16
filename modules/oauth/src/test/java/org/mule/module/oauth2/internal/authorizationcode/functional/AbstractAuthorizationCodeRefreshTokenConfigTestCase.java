/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.module.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.module.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.tck.junit4.rule.SystemProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;

public class AbstractAuthorizationCodeRefreshTokenConfigTestCase extends AbstractOAuthAuthorizationTestCase
{

    private static final String RESOURCE_PATH = "/resource";
    public static final String RESOURCE_RESULT = "resource result";
    public static final String REFRESHED_ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4guasdfsdfa";

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

    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-refresh-token-config.xml";
    }

    protected void executeRefreshToken(String flowName, String oauthConfigName, String userId, int failureStatusCode) throws Exception
    {
        configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(REFRESHED_ACCESS_TOKEN);

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

        final ConfigOAuthContext configOAuthContext = muleContext.getRegistry().<TokenManagerConfig>lookupObject(oauthConfigName).getConfigOAuthContext();
        final ResourceOwnerOAuthContext resourceOwnerOauthContext = configOAuthContext.getContextForResourceOwner(userId);
        resourceOwnerOauthContext.setAccessToken(ACCESS_TOKEN);
        resourceOwnerOauthContext.setRefreshToken(REFRESH_TOKEN);
        configOAuthContext.updateResourceOwnerOAuthContext(resourceOwnerOauthContext);


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

