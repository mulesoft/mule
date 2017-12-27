/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.clientcredentials.functional.domain;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase.ACCESS_TOKEN;
import static org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase.EXPIRES_IN;
import static org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase.TOKEN_PATH;
import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.tck.junit4.DomainFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Rule;
import org.junit.Test;

public class ClientCredentialsDomainTestCase extends DomainFunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Rule
    public DynamicPort oauthServerPort = new DynamicPort("oauthServerPort");

    @Rule
    public SystemProperty tokenUrl = new SystemProperty("tokenUrl", format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(oauthServerPort.getNumber()));

    @Override
    protected String getDomainConfig()
    {
        return "client-credentials/domain/oauth-domain-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {};
    }

    public void setUpMuleContexts() throws Exception
    {
        wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH))
                                     .willReturn(aResponse()
                                                         .withBody("{" +
                                                                   "\"" + OAuthConstants.ACCESS_TOKEN_PARAMETER + "\":\"" + ACCESS_TOKEN + "\"," +
                                                                   "\"" + OAuthConstants.EXPIRES_IN_PARAMETER + "\":\"" + EXPIRES_IN + "\"}")));

        super.setUpMuleContexts();
    }


    @Test
    public void oauthAsSharedResource()
    {
        wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH)));
    }

}
