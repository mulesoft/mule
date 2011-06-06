/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.config;

public class MysqlDataSourceFactoryBean extends AbstractDataSourceFactoryBean
{
    private static final String DEFAULT_HOST = "localhost";
    private static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
    private static final String JDBC_URL_PREFIX = "jdbc:mysql://";

    protected String database;
    protected String host = DEFAULT_HOST;
    protected int port = -1;

    public MysqlDataSourceFactoryBean()
    {
        super();
        driverClassName = DRIVER_CLASS_NAME;
        updateUrl();
    }

    protected void updateUrl()
    {
        StringBuilder buf = new StringBuilder(JDBC_URL_PREFIX);
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
