/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.domain.database;

import org.mule.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class MySqlConfigFactoryBean extends DbConfigFactoryBean
{

    private static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
    private static final String JDBC_URL_PREFIX = "jdbc:mysql://";

    private String database;
    private String host;
    private int port = -1;

    public MySqlConfigFactoryBean()
    {
        super();
        setDriverClassName(DRIVER_CLASS_NAME);
    }

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

    @Override
    protected String getEffectiveUrl()
    {
        String url = getUrl();
        if (StringUtils.isEmpty(url))
        {
            StringBuilder buf = new StringBuilder(128);
            buf.append(getUrlPrefix());
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

        if (DRIVER_CLASS_NAME.equals(getDriverClassName()))
        {
            url = forceParamMetadataGeneration(url);
        }

        return url;
    }

    private String forceParamMetadataGeneration(String url)
    {
        StringBuilder buf = new StringBuilder(128);
        buf.append(url);
        try
        {
            URI uri = new URI(url.substring(5));
            String query = uri.getQuery();
            if (query == null)
            {
                buf.append("?");
            }
            else
            {
                buf.append("&");
            }

            buf.append("generateSimpleParameterMetadata=true");
            url = buf.toString();
        }
        catch (URISyntaxException e)
        {
            throw new IllegalArgumentException("Unable to parse database config URL", e);
        }
        return url;
    }

    public String getUrlPrefix()
    {
        return JDBC_URL_PREFIX;
    }

    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }
}