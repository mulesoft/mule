/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.api.MuleRuntimeException;
import org.mule.module.http.api.HttpConstants;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerAddress
{

    private final String ip;
    private String host;
    private int port;

    public ServerAddress(String host, int port)
    {
        try
        {
            this.host = host;
            this.port = port;
            this.ip = InetAddress.getByName(host).getHostAddress();
        }
        catch (UnknownHostException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getIp()
    {
        return ip;
    }

    public boolean overlaps(ServerAddress serverAddress)
    {
        return (port == serverAddress.getPort()) &&
               (isAllInterfaces() || serverAddress.isAllInterfaces());
    }

    public boolean isAllInterfaces()
    {
        return host.equals(HttpConstants.ALL_INTERFACES_IP);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        ServerAddress that = (ServerAddress) o;

        if (port != that.port)
        {
            return false;
        }
        if (!ip.equals(that.ip))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = ip.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString()
    {
        return "ServerAddress{" +
               "ip='" + ip + '\'' +
               ", host='" + host + '\'' +
               ", port=" + port +
               '}';
    }
}
