/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.config;

import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

/**
 * A Proxy configuration for the SFTP connector.
 *
 * @since 3.9
 */
public class SftpProxyConfig
{
    public static final String USERNAME_PROPERTY = SYSTEM_PROPERTY_PREFIX + "sftp.proxy.username";
    public static final String PASSWORD_PROPERTY = SYSTEM_PROPERTY_PREFIX + "sftp.proxy.username";
    public static final String HOST_PROPERTY = SYSTEM_PROPERTY_PREFIX + "sftp.proxy.host";
    public static final String PORT_PROPERTY = SYSTEM_PROPERTY_PREFIX + "sftp.proxy.port";
    public static final String PROTOCOL_PROPERTY = SYSTEM_PROPERTY_PREFIX + "sftp.proxy.protocol";

    public enum Protocol
    {
        HTTP,
        SOCKS4,
        SOCKS5
    };

    private String host;
    private int port;
    private String username;
    private String password;
    private Protocol protocol;

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public Protocol getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = Protocol.valueOf(protocol);
    }
}
