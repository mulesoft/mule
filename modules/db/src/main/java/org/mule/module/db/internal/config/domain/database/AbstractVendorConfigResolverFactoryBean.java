/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import org.mule.module.db.internal.domain.database.ConfigurableDbConfigFactory;
import org.mule.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 *  Base class for creating vendor's {@link DbConfigResolverFactoryBean}
 */
public abstract class AbstractVendorConfigResolverFactoryBean extends DbConfigResolverFactoryBean
{

    private String host;
    private int port = -1;
    private final String urlPrefix;
    private String database;

    protected AbstractVendorConfigResolverFactoryBean(String urlPrefix, ConfigurableDbConfigFactory dbConfigFactory)
    {
        super(dbConfigFactory);
        this.urlPrefix = urlPrefix;
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

    public String getUrlPrefix()
    {
        return urlPrefix;
    }

    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }

    @Override
    protected String getEffectiveUrl()
    {
        String url = getUrl();
        if (StringUtils.isEmpty(url))
        {
            url = buildUrlFromAttributes();
        }

        return addProperties(url);
    }

    protected String buildUrlFromAttributes()
    {
        String url;
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
        return url;
    }

    private String addProperties(String url)
    {
        Map<String, String> connectionProperties = getConnectionProperties();

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
}
