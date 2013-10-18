/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
