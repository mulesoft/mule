/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.functional.matcher.HttpMessageAttributesMatchers.hasStatusCode;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.NetworkUtils;

import com.ning.http.client.ntlm.NTLMEngine;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractNtlmTestCase extends AbstractHttpRequestTestCase
{
    private static final String TYPE_1_MESSAGE = "NTLM TlRMTVNTUAABAAAAAYIIogAAAAAoAAAAAAAAACgAAAAFASgKAAAADw==";
    private static final String TYPE_2_MESSAGE_CHALLENGE = "TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==";
    private static final String TYPE_2_MESSAGE = "NTLM " + TYPE_2_MESSAGE_CHALLENGE;
    private static final String USER = "Zaphod";
    private static final String PASSWORD = "Beeblebrox";
    private static final String DOMAIN = "Ursa-Minor";
    private static final String AUTHORIZED = "Authorized";

    private String type3Message;
    protected String requestUrl;

    private String clientAuthHeader;
    private String serverAuthHeader;
    private int unauthorizedHeader;

    public AbstractNtlmTestCase(String clientAuthHeader, String serverAuthHeader, int unauthorizedHeader)
    {
        this.clientAuthHeader = clientAuthHeader;
        this.serverAuthHeader = serverAuthHeader;
        this.unauthorizedHeader = unauthorizedHeader;
    }

    @Before
    public void setUp() throws Exception
    {
        String workstation = getWorkstation();
        String ntlmHost = workstation != null ? workstation : NetworkUtils.getLocalHost().getHostName();
        String type3Challenge = NTLMEngine.INSTANCE.generateType3Msg(USER, PASSWORD, getDomain(), ntlmHost, TYPE_2_MESSAGE_CHALLENGE);
        type3Message = "NTLM " + type3Challenge;
    }

    protected String getDomain()
    {
        return DOMAIN;
    }

    protected String getWorkstation()
    {
        return null;
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String auth = request.getHeader(clientAuthHeader);
        if (auth == null)
        {
            response.setStatus(unauthorizedHeader);
            response.addHeader(serverAuthHeader, "NTLM");
        }
        if (TYPE_1_MESSAGE.equals(auth))
        {
            response.setStatus(unauthorizedHeader);
            response.setHeader(serverAuthHeader, TYPE_2_MESSAGE);
        }
        else if (type3Message.equals(auth))
        {
            requestUrl = baseRequest.getRequestURL().toString();
            response.setStatus(SC_OK);
            response.getWriter().print(AUTHORIZED);
        }
        else
        {
            response.setStatus(SC_UNAUTHORIZED);
        }
    }

    @Test
    public void validNtlmAuth() throws Exception
    {
        MuleMessage response = runFlow(getFlowName()).getMessage();

        assertThat((HttpResponseAttributes) response.getAttributes(), hasStatusCode(SC_OK));
        assertThat(getPayloadAsString(response), equalTo(AUTHORIZED));
    }

    protected String getFlowName()
    {
        return "ntlmFlow";
    }
}
