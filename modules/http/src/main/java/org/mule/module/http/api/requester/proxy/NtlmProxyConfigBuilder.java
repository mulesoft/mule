/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester.proxy;

import static org.mule.util.Preconditions.checkArgument;

import org.mule.module.http.internal.request.DefaultProxyConfig;
import org.mule.module.http.internal.request.NtlmProxyConfig;

/**
 * Builder for an http proxy using NTLM.
 */
public class NtlmProxyConfigBuilder
{

    public static final int MAXIMUM_PORT_NUMBER = 65535;
    private NtlmProxyConfig ntlmProxyConfig = new NtlmProxyConfig();

    /**
     * @param host proxy host
     * @return the builder
     */
    public NtlmProxyConfigBuilder setHost(String host)
    {
        ntlmProxyConfig.setHost(host);
        return this;
    }

    /**
     * @param port proxy port
     * @return the builder
     */
    public NtlmProxyConfigBuilder setPort(int port)
    {
        ntlmProxyConfig.setPort(port);
        return this;
    }

    /**
     * @param username proxy authentication username
     * @return the builder
     */
    public NtlmProxyConfigBuilder setUsername(String username)
    {
        ntlmProxyConfig.setUsername(username);
        return this;
    }

    /**
     * @param password proxy authentication password
     * @return the builder
     */
    public NtlmProxyConfigBuilder setPassword(String password)
    {
        ntlmProxyConfig.setPassword(password);
        return this;
    }

    public ProxyConfig build()
    {
        checkArgument(ntlmProxyConfig.getHost() != null, "Host must be not null");
        checkArgument(ntlmProxyConfig.getPort() <= MAXIMUM_PORT_NUMBER, "Port was not configured or configured with a value greater than " + MAXIMUM_PORT_NUMBER);
        checkArgument(ntlmProxyConfig.getNtlmDomain() != null, "Ntlm domain must be not null");
        return ntlmProxyConfig;
    }

    /**
     * @param ntlmDomain proxy ntlm domain
     * @return the builder
     */
    public NtlmProxyConfigBuilder setNtlmDomain(String ntlmDomain)
    {
        ntlmProxyConfig.setNtlmDomain(ntlmDomain);
        return this;
    }

}
