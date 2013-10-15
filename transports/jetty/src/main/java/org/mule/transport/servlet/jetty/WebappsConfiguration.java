/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

public class WebappsConfiguration
{
    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final int DEFAULT_PORT = 8585;

    private String host = DEFAULT_HOST;
    private String directory;
    private int port = DEFAULT_PORT;
    private String[] systemClasses;
    private String[] serverClasses;

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getDirectory()
    {
        return directory;
    }

    public void setDirectory(String directory)
    {
        this.directory = directory;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String[] getSystemClasses()
    {
        return systemClasses;
    }

    public void setSystemClasses(String[] systemClasses)
    {
        this.systemClasses = systemClasses;
    }

    public String[] getServerClasses()
    {
        return serverClasses;
    }

    public void setServerClasses(String[] serverClasses)
    {
        this.serverClasses = serverClasses;
    }
}
