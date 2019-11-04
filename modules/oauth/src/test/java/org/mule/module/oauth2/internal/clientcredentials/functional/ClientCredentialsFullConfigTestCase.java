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
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.module.oauth2.internal.AbstractGrantType.buildAuthorizationHeaderContent;
import org.mule.api.MuleRuntimeException;
import org.mule.construct.Flow;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.module.oauth2.asserter.OAuthContextFunctionAsserter;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.store.SimpleMemoryObjectStore;

import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ClientCredentialsFullConfigTestCase extends AbstractOAuthAuthorizationTestCase
{

    private final String CUSTOM_RESPONSE_PARAMETER1_VALUE = "token-resp-value1";
    private final String CUSTOM_RESPONSE_PARAMETER2_VALUE = "token-resp-value2";
    private static final String RESOURCE_PATH = "/resource";
    private static final String NEW_ACCESS_TOKEN = "abcdefghjkl";
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("%s://localhost:%d" + TOKEN_PATH, getProtocol(), oauthHttpsServerPort.getNumber()));
    @Rule
    public SystemProperty customTokenResponseParameter1Name = new SystemProperty("custom.param.extractor1", "token-resp-param1");
    @Rule
    public SystemProperty customTokenResponseParameter2Name = new SystemProperty("custom.param.extractor2", "token-resp-param2");
    @Rule
    public SystemProperty maxRetriesProperty;

    private String configFile;

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    public ClientCredentialsFullConfigTestCase(String configFile, String clientMaxRetries)
    {
        this.configFile = configFile;
        this.maxRetriesProperty = new SystemProperty("mule.http.client.maxRetries", clientMaxRetries);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[] {"client-credentials/client-credentials-full-config-tls-global.xml", "0"},
                             new Object[] {"client-credentials/client-credentials-full-config-tls-nested.xml", "0"},
                             new Object[] {"client-credentials/client-credentials-full-config-tls-global.xml", "3"},
                             new Object[] {"client-credentials/client-credentials-full-config-tls-nested.xml", "3"});
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        final ImmutableMap customTokenResponseParameters = new ImmutableMap.Builder()
                .put(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE)
                .put(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE).build();
        configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(customTokenResponseParameters);
    }

    @Test
    public void authenticationIsDoneOnStartupUsingScope() throws Exception
    {
        verifyRequestDoneToTokenUrlForClientCredentials(scopes.getValue());

        SimpleMemoryObjectStore objectStore = muleContext.getRegistry().get("customObjectStore");
        assertThat(objectStore.allKeys().isEmpty(), is(false));
        ResourceOwnerOAuthContext resourceOwnerOAuthContext = (ResourceOwnerOAuthContext) objectStore.retrieve(ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID);
        assertThat(resourceOwnerOAuthContext.getAccessToken(), Is.<Serializable>is(ACCESS_TOKEN));
    }

    @Test
    public void customTokenResponseParametersAreCaptured() throws Exception
    {
        final OAuthContextFunctionAsserter oauthContextAsserter = OAuthContextFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), "tokenManagerConfig");
        oauthContextAsserter.assertAccessTokenIs(ACCESS_TOKEN);
        oauthContextAsserter.assertExpiresInIs(EXPIRES_IN);
        oauthContextAsserter.assertContainsCustomTokenResponseParam(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE);
        oauthContextAsserter.assertContainsCustomTokenResponseParam(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE);
    }

    @Test
    public void authenticationFailedTriggersRefreshAccessToken() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(NEW_ACCESS_TOKEN);

        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION, containing(ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withStatus(500).withHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"myRealm\"")));

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

    @Test
    public void authenticationFailedTriggersRefreshAccessTokenOnceAtATime() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(NEW_ACCESS_TOKEN);
        
        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                .withHeader(HttpHeaders.Names.AUTHORIZATION, containing(ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withStatus(500).withHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"myRealm\"")));
        
        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                .withHeader(HttpHeaders.Names.AUTHORIZATION, containing(NEW_ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withBody(TEST_MESSAGE)
                        .withStatus(200)));
        
        final Flow testFlow = (Flow) getFlowConstruct("testFlow");
        
        ExecutorService executor = newFixedThreadPool(2);
        try
        {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < 2; ++i)
            {
                futures.add(executor.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            testFlow.process(getTestEvent(TEST_MESSAGE));
                        }
                        catch (Exception e)
                        {
                            throw new MuleRuntimeException(e);
                        }
                    }
                }));
            }
            
            for (Future<?> future : futures)
            {
                future.get(RECEIVE_TIMEOUT, MILLISECONDS);
            }
            
            verifyRequestDoneToTokenUrlForClientCredentials();
            
            // 2 from the app start, only 1 for the 2 flows
            wireMockRule.verify(3, postRequestedFor(urlEqualTo(TOKEN_PATH)));
        }
        finally
        {
            executor.shutdownNow();
        }
    }
    
    @Override
    protected String getProtocol()
    {
        return HTTPS.getScheme();
    }
}
