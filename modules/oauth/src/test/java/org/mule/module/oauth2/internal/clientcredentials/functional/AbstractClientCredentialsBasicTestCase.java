/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.clientcredentials.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mule.module.oauth2.internal.AbstractGrantType.buildAuthorizationHeaderContent;

import org.mule.construct.Flow;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.module.oauth2.internal.AbstractGrantType;
import org.mule.module.oauth2.internal.authorizationcode.OAuthAuthenticationHeader;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractClientCredentialsBasicTestCase extends AbstractOAuthAuthorizationTestCase
{

    private static final String RESOURCE_PATH = "/resource";
    private static final String NEW_ACCESS_TOKEN = "abcdefghjkl";
    public static final String TEST_FLOW_NAME = "testFlow";
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

    @Override
    protected String getConfigFile()
    {
        return "client-credentials/client-credentials-minimal-config.xml";
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType();
    }

    @Test
    public void authenticationHeaderIsSentWithRequest() throws Exception
    {
        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .willReturn(aResponse()
                                                         .withBody(TEST_MESSAGE)));

        Flow testFlow = (Flow) getFlowConstruct(TEST_FLOW_NAME);
        testFlow.process(getTestEvent(TEST_MESSAGE));

        wireMockRule.verify(postRequestedFor(urlEqualTo(RESOURCE_PATH))
                                    .withHeader(HttpHeaders.Names.AUTHORIZATION, equalTo(buildAuthorizationHeaderContent(ACCESS_TOKEN))));
    }

    @Test
    public void authenticationFailedTriggersRefreshAccessToken() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType(NEW_ACCESS_TOKEN);

        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION, containing(ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withStatus(401).withHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"myRealm\"")));

        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION, containing(NEW_ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withBody(TEST_MESSAGE)
                                                         .withStatus(200)));

        Flow testFlow = (Flow) getFlowConstruct("testFlow");
        testFlow.process(getTestEvent(TEST_MESSAGE));

        verifyRequestDoneToTokenUrlForClientCredentials();

        wireMockRule.verify(postRequestedFor(urlEqualTo(RESOURCE_PATH))
                                    .withHeader(HttpHeaders.Names.AUTHORIZATION, equalTo(buildAuthorizationHeaderContent(NEW_ACCESS_TOKEN))));
    }
}
