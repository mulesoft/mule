/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester.proxy;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.commons.io.IOUtils;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.NetworkUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ning.http.client.ntlm.NTLMEngine;

/**
 * Authorizer to handle NTLM authorizations in tests
 */
public class NtlmProxyTestAuthorizer implements TestAuthorizer
{
    private static final Logger LOGGER = getLogger(NtlmProxyTestAuthorizer.class);

    private static final String TYPE_2_MESSAGE_CHALLENGE = "TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==";
    private static final String TYPE_2_MESSAGE = "NTLM " + TYPE_2_MESSAGE_CHALLENGE;
    private static final String TYPE_1_MESSAGE = "NTLM TlRMTVNTUAABAAAAAYIIogAAAAAoAAAAAAAAACgAAAAFASgKAAAADw==";
    private static final String AUTHORIZED = "Authorized";

    private String clientAuthHeader;
    private String serverAuthHeader;
    private int unauthorizedHeader;
    private String user;
    private String password;
    private String domain;
    private String workstation;

    private String type3Message;
    private String payloadAfterDancing;


    public NtlmProxyTestAuthorizer(String clientAuthHeader, String serverAuthHeader, int unauthorizedHeader, String user, String password, String domain, String workstation)
    {
        this.clientAuthHeader = clientAuthHeader;
        this.serverAuthHeader = serverAuthHeader;
        this.unauthorizedHeader = unauthorizedHeader;
        this.user = user;
        this.password = password;
        this.domain = domain;
        this.workstation = workstation;
    }

    public void setUp() throws Exception
    {
        String ntlmHost = workstation != null ? workstation : NetworkUtils.getLocalHost().getHostName();
        String type3Challenge = NTLMEngine.INSTANCE.generateType3Msg(user, password, domain, ntlmHost, TYPE_2_MESSAGE_CHALLENGE);
        type3Message = "NTLM " + type3Challenge;
    }

    @Override
    public boolean authorizeRequest(String address, HttpServletRequest request, HttpServletResponse response, boolean addAuthorizeMessageInProxy) throws IOException
    {
        String auth = request.getHeader(clientAuthHeader);
        if (auth == null)
        {
            response.setStatus(unauthorizedHeader);
            response.addHeader(serverAuthHeader, "NTLM");
            return false;
        }
        if (TYPE_1_MESSAGE.equals(auth))
        {
            response.setStatus(unauthorizedHeader);
            response.setHeader(serverAuthHeader, TYPE_2_MESSAGE);
            return false;
        }
        else if (type3Message.equals(auth))
        {
            response.setStatus(SC_OK);
            extractPayload(request.getInputStream());
            if (addAuthorizeMessageInProxy)
            {
                response.getWriter().print(AUTHORIZED);
            }
            return true;
        }
        else
        {
            extractPayload(request.getInputStream());
            response.setStatus(SC_UNAUTHORIZED);
            return false;
        }
    }

    protected void extractPayload(InputStream content)
    {
        try
        {
            payloadAfterDancing = new String(toByteArray(content));
        }
        catch (IOException e)
        {
            LOGGER.warn("Exception on extracting content of request {}", e.getMessage());
        }
    }


    public static class Builder
    {
        private String clientAuthHeader;
        private String serverAuthHeader;
        private int unauthorizedHeader;
        private String user;
        private String password;
        private String domain;
        private String workstation;

        public Builder setClientAuthHeader(String clientAuthHeader)
        {
            this.clientAuthHeader = clientAuthHeader;
            return this;
        }

        public Builder setServerAuthHeader(String serverAuthHeader)
        {
            this.serverAuthHeader = serverAuthHeader;
            return this;
        }

        public Builder setUnauthorizedHeader(int unauthorizedHeader)
        {
            this.unauthorizedHeader = unauthorizedHeader;
            return this;
        }

        public Builder setUser(String user)
        {
            this.user = user;
            return this;
        }

        public Builder setPassword(String password)
        {
            this.password = password;
            return this;
        }

        public Builder setDomain(String domain)
        {
            this.domain = domain;
            return this;
        }

        public Builder setWorkstation(String workstation)
        {
            this.workstation = workstation;
            return this;
        }

        public NtlmProxyTestAuthorizer build() throws Exception
        {
            NtlmProxyTestAuthorizer testAuthorizer = new NtlmProxyTestAuthorizer(clientAuthHeader, serverAuthHeader, unauthorizedHeader, user, password, domain, workstation);
            testAuthorizer.setUp();

            return testAuthorizer;
        }
    }

    public String getPayloadAfterDancing()
    {
        return payloadAfterDancing;
    }


}
