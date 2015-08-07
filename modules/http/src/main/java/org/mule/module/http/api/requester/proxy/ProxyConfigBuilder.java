/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester.proxy;

import org.mule.module.http.internal.request.DefaultProxyConfig;
import org.mule.util.Preconditions;

/**
 * Builder for http proxy configuration.
 */
public class ProxyConfigBuilder
{

    public static final int MAXIMUM_PORT_NUMBER = 65535;
    private DefaultProxyConfig proxyConfig = new DefaultProxyConfig();

    public void setName(String name)
    {
        proxyConfig.setName(name);
    }

    public ProxyConfigBuilder setHost(String host)
    {
        proxyConfig.setHost(host);
        return this;
    }

    public ProxyConfigBuilder setPort(int port)
    {
        proxyConfig.setPort(port);
        return this;
    }

    public ProxyConfigBuilder setUsername(String username)
    {
        proxyConfig.setUsername(username);
        return this;
    }

    public ProxyConfigBuilder setPassword(String password)
    {
        proxyConfig.setPassword(password);
        return this;
    }

    public ProxyConfig build()
    {
        Preconditions.checkArgument(proxyConfig.getHost() != null, "Host must be not null");
        Preconditions.checkArgument(proxyConfig.getPort() <= MAXIMUM_PORT_NUMBER, "Port must be under 65535. Check that you set the port.");
        return proxyConfig;
    }

}
