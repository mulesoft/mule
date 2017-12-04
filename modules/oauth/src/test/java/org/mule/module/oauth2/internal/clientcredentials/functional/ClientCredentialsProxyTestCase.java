/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.clientcredentials.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mule.tck.functional.FlowAssert.verify;

import org.junit.Rule;
import org.junit.Test;

import org.mule.module.http.api.HttpHeaders;
import org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;


public class ClientCredentialsProxyTestCase extends AbstractOAuthAuthorizationTestCase
{

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

    @Rule
    public SystemProperty proxyCount = new SystemProperty("proxyCount", "2");

    @Override
    protected String [] getConfigFiles()
    {
        return new String[] {"proxy/oauth-proxy-template.xml", "client-credentials/client-credentials-through-proxy-config.xml"};
    }

    @Override
    public void doSetUpBeforeMuleContextCreation()
    {
        configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(ACCESS_TOKEN);
        wireMockRule.stubFor(post(urlEqualTo("/resource"))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION, containing(ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withBody(TEST_MESSAGE)
                                                         .withStatus(200)));
    }

    @Test
    public void tokenRequestThroughProxy() throws Exception
    {
        verify("proxyTemplate");
    }

}
