/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
