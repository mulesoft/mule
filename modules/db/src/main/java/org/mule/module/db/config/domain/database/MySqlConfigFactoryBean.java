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
import java.util.Map;

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

        return addProperties(url);
    }

    private String addProperties(String url)
    {
        Map<String, String> connectionProperties = getConnectionProperties();

        if (DRIVER_CLASS_NAME.equals(getDriverClassName()))
        {
            connectionProperties.put("generateSimpleParameterMetadata", "true");
        }

        if (connectionProperties.isEmpty())
        {
            return url;
        }

        StringBuilder effectiveUrl = new StringBuilder(url);

        if (getUri(url).getQuery() == null)
        {
            effectiveUrl.append("?");
        }
        else
        {
            effectiveUrl.append("&");
        }

        return effectiveUrl.append(buildQueryParams(connectionProperties)).toString();
    }

    private String buildQueryParams(Map<String, String> connectionProperties)
    {
        StringBuilder params = new StringBuilder(128);
        for (Map.Entry<String, String> entry : connectionProperties.entrySet())
        {
            if (params.length() > 0)
            {
                params.append('&');
            }

            params.append(entry.getKey())
                    .append('=')
                    .append(entry.getValue());
        }

        return params.toString();
    }

    private URI getUri(String url)
    {
        try
        {
            return new URI(url.substring(5));
        }
        catch (URISyntaxException e)
        {
            throw new IllegalArgumentException("Unable to parse database config URL", e);
        }
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