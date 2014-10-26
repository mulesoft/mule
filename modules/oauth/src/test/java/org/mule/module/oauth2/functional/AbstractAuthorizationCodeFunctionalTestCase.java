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

import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;

public abstract class AbstractAuthorizationCodeFunctionalTestCase extends FunctionalTestCase
{

    public static final int REQUEST_TIMEOUT = 1000;

    public static final String TOKEN_PATH = "/token";
    public static final String AUTHENTICATION_CODE = "9WGJOBZXAvSibONGAxVlLuML0e0RhfX4";
    public static final String ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4gu6vpCobc2ya";
    public static final String REFRESH_TOKEN = "cry825cyCs2O0j7tRXXVS4AXNu7hsO5wbWjcBoFFcJePy5zZwuQEevIp6hsUaywp";
    public static final String EXPIRES_IN = "3897";
    public static final String AUTHORIZE_PATH = "/authorize";
    protected final DynamicPort localHostPort = new DynamicPort("port1");
    protected final DynamicPort oauthServerPort = new DynamicPort("port2");
    protected final DynamicPort oauthHttpsServerPort = new DynamicPort("port3");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(oauthServerPort.getNumber(), oauthHttpsServerPort.getNumber());
    @Rule
    public SystemProperty clientId = new SystemProperty("client.id", "ndli93xdws2qoe6ms1d389vl6bxquv3e");
    @Rule
    public SystemProperty clientSecret = new SystemProperty("client.secret", "yL692Az1cNhfk1VhTzyx4jOjjMKBrO9T");
    @Rule
    public SystemProperty scopes = new SystemProperty("scopes", "expected scope");
    @Rule
    public SystemProperty state = new SystemProperty("state", "expected state");
    @Rule
    public SystemProperty oauthServerPortNumber = new SystemProperty("oauth.server.port", String.valueOf(oauthServerPort.getNumber()));
    @Rule
    public SystemProperty redirectUrl = new SystemProperty("redirect.url", String.format("%s://localhost:%d/redirect", getProtocol(), localHostPort.getNumber()));

    protected String getProtocol()
    {
        return "http";
    }

    protected void configureWireMockToExpectTokenPathRequestAndReturnJsonResponse()
    {
        configureWireMockToExpectTokenPathRequestAndReturnJsonResponse(ACCESS_TOKEN);
    }

    protected void configureWireMockToExpectTokenPathRequestAndReturnJsonResponse(String accessToken)
    {
        wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH))
                                     .willReturn(aResponse()
                                                         .withBody("{" +
                                                                   "\"" + OAuthConstants.ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," +
                                                                   "\"" + OAuthConstants.EXPIRES_IN_PARAMETER + "\":" + EXPIRES_IN + "," +
                                                                   "\"" + OAuthConstants.REFRESH_TOKEN_PARAMETER + "\":\"" + REFRESH_TOKEN + "\"}")));
    }

    protected void verifyRequestDoneToTokenUrl() throws UnsupportedEncodingException
    {
        wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_ID_PARAMETER + "=" + URLEncoder.encode(clientId.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CODE_PARAMETER + "=" + URLEncoder.encode(AUTHENTICATION_CODE, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_SECRET_PARAMETER + "=" + URLEncoder.encode(clientSecret.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.GRANT_TYPE_PARAMETER + "=" + URLEncoder.encode(OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.REDIRECT_URI_PARAMETER + "=" + URLEncoder.encode(redirectUrl.getValue(), StandardCharsets.UTF_8.name()))));
    }

}
