/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.module.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.util.NetworkUtils;

import com.ning.http.client.ntlm.NTLMEngine;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpRequestNtlmAuthTestCase extends AbstractHttpRequestTestCase
{

    private static final String TYPE_1_MESSAGE = "NTLM TlRMTVNTUAABAAAAAYIIogAAAAAoAAAAAAAAACgAAAAFASgKAAAADw==";
    private static final String TYPE_2_MESSAGE_CHALLENGE = "TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==";
    private static final String TYPE_2_MESSAGE = "NTLM " + TYPE_2_MESSAGE_CHALLENGE;
    private static final String USER = "Zaphod";
    private static final String PASSWORD = "Beeblebrox";
    private static final String AUTHORIZED = "Authorized";

    private String type3Message;

    @Parameterized.Parameter(0)
    public String flowName;

    @Parameterized.Parameter(1)
    public String domain;

    @Parameterized.Parameter(2)
    public String workstation;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"ntlmAuthRequestWithDomain", "Ursa-Minor", null},
                {"ntlmAuthRequestWithoutDomain", "", null},
                {"ntlmAuthRequestWithWorkstation", "Ursa-Minor", "LightCity"}});
    }

    @Before
    public void setUp() throws Exception
    {
        String ntlmHost = workstation != null ? workstation : NetworkUtils.getLocalHost().getHostName();
        String type3Challenge = NTLMEngine.INSTANCE.generateType3Msg(USER, PASSWORD, domain, ntlmHost, TYPE_2_MESSAGE_CHALLENGE);
        type3Message = "NTLM " + type3Challenge;
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-ntlm-auth-config.xml";
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String auth = request.getHeader(AUTHORIZATION);
        if (auth == null)
        {
            response.setStatus(SC_UNAUTHORIZED);
            response.addHeader(WWW_AUTHENTICATE, "NTLM");
        }
        if (TYPE_1_MESSAGE.equals(auth))
        {
            response.setStatus(SC_UNAUTHORIZED);
            response.setHeader(WWW_AUTHENTICATE, TYPE_2_MESSAGE);
        }
        else if (type3Message.equals(auth))
        {
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
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event = flow.process(event);

        assertThat((int) event.getMessage().getInboundProperty("http.status"), is(SC_OK));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(AUTHORIZED));
    }

}
