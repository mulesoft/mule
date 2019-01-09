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
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.AUTHORIZATION;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

public class ClientCredentialsTokenRequestConcurrentTestCase extends AbstractOAuthAuthorizationTestCase
{

    private CountDownLatch latch = new CountDownLatch(NUM_REQUESTS);

    private static final int NUM_REQUESTS = 100;

    private static final String STATUS = "http.status";

    private int responseCount = 0;

    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

    @Override
    protected String getConfigFile()
    {
        return "client-credentials/client-credentials-token-request-race-condition-config.xml";
    }

    @Override
    public void doSetUpBeforeMuleContextCreation()
    {
        configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(ACCESS_TOKEN);
        wireMockRule.stubFor(post(urlEqualTo("/resource"))
                                                          .withHeader(AUTHORIZATION, containing(ACCESS_TOKEN))
                                                          .willReturn(aResponse()
                                                                                 .withBody(TEST_MESSAGE)
                                                                                 .withStatus(SC_UNAUTHORIZED)));
    }

    /**
     * A Token refresh will be attempted every time it gets a 401. This previously result in an error different from 401
     * if a new token was required when the token was removed from the object store during the refresh process.
     * 
     * @throws Exception
     */
    @Test
    public void raceConditionNotExceptionRaisedOnRefreshToken() throws Exception
    {
        Set<Thread> threads = new HashSet<Thread>();

        for (int i = 0; i < NUM_REQUESTS; i++)
        {
            Thread thread = new Thread(new TestForRaceConditionRunnable(), "thread" + i);
            threads.add(thread);
            thread.start();
        }

        latch.await();

        assertThat(responseCount, equalTo(NUM_REQUESTS));
    }

    private class TestForRaceConditionRunnable implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                MuleEvent event = runFlow("request", getTestEvent(TEST_MESSAGE));
                if (event.getMessage().<Integer> getInboundProperty(STATUS) == SC_UNAUTHORIZED)
                {
                    responseCount++;
                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                latch.countDown();
            }
        }
    }
}
