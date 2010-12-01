
package org.mule.transport.servlet.jetty;

public class WebappsConfiguration
{
    private String host;
    private String directory;
    private int port;
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
