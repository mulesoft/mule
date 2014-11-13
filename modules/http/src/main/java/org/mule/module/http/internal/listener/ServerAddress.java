/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

public class ServerAddress
{

    private String host;
    private int port;

    public ServerAddress(String host, int port)
    {
        this.host = host;
        this.port = port;
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
        if (!host.equals(that.host))
        {
            return false;
        }

        return true;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public int hashCode()
    {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString()
    {
        return "ServerAddress{" +
               "host='" + host + '\'' +
               ", port=" + port +
               '}';
    }
}
