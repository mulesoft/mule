/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.is;
import static org.mule.module.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.module.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.module.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Values.CLOSE;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

public class HttpRequestsWithNtlmAuthenticationConnectionPersistenceTestCase extends AbstractNtlmTestCase
{

    private static final String STATIC_CONFIGURED_NTLM_AUTH_FLOW = "staticConfiguredNtlmAuth";
    private static final String DYNAMIC_CONFIGURED_NTLM_AUTH_FLOW = "dynamicConfiguredNtlmAuth";
    private final String NTLM_PASSWORD = "Beeblebrox";
    private boolean lastDanceRequestHasConnectionClose = false;
    private AtomicInteger seenMessages = new AtomicInteger(0);

    @Before
    public void clearConnectionCloseObservers()
    {
        lastDanceRequestHasConnectionClose = false;
        seenMessages.set(0);
    }

    @Override
    protected String getWorkstation()
    {
        return null;
    }

    @Override
    protected void handleRequest(String address, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if (seenMessages.get() == 2)
        {
            //This is the third and last message
            String connectionHeader = request.getHeader(CONNECTION);
            lastDanceRequestHasConnectionClose = connectionHeader != null && connectionHeader.equalsIgnoreCase(CLOSE);
        }
        super.handleRequest(address, request, response);
        seenMessages.incrementAndGet();
    }

    @Override
    protected String getDomain()
    {
        return "";
    }

    /**
     * This test will verify that two request with NTLM-authentication can be made consequently.
     *
     * @return the flow name for the test defined {@link AbstractNtlmTestCase}.
     */
    @Override
    protected String getFlowName()
    {
        return STATIC_CONFIGURED_NTLM_AUTH_FLOW;
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-volatile-ntlm-auth-config.xml";
    }

    @Before
    public void regenerateTestAuthorizer()
    {
        setupTestAuthorizer(AUTHORIZATION, WWW_AUTHENTICATE, SC_UNAUTHORIZED);
    }

    @Test
    public void requestWithStaticCredentialsDoesNotSendConnectionClose() throws Exception
    {
        runFlow(STATIC_CONFIGURED_NTLM_AUTH_FLOW);
        assertThat(lastDanceRequestHasConnectionClose, is(false));
    }

    @Test
    public void requestWithDynamicCredentialsSendsConnectionClose() throws Exception
    {
        runFlow(DYNAMIC_CONFIGURED_NTLM_AUTH_FLOW, NTLM_PASSWORD);
        assertThat(lastDanceRequestHasConnectionClose, is(true));
    }
}
