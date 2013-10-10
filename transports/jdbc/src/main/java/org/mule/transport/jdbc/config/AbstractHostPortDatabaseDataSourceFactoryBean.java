/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.config;

public abstract class AbstractHostPortDatabaseDataSourceFactoryBean extends AbstractDataSourceFactoryBean
{
    protected  final String DEFAULT_HOST = "localhost";

    protected String database;
    protected String host;
    protected int port = -1;

    public AbstractHostPortDatabaseDataSourceFactoryBean()
    {
        super();
        host = DEFAULT_HOST;
    }

    protected void updateUrl()
    {
        StringBuilder buf = new StringBuilder(64);
        buf.append(getJdbcUrlPrefix());
        buf.append(getHost());
        if (getPort() > 0)
        {
            buf.append(":");
            buf.append(getPort());
        }
        buf.append("/");
        buf.append(getDatabase());

        url = buf.toString();
    }

    protected abstract String getJdbcUrlPrefix();

    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
        updateUrl();
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
        updateUrl();
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
        updateUrl();
    }
}
