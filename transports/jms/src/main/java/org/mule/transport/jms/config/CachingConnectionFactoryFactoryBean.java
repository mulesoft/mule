/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.config;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * DEPRECATED: This element is deprecated from Mule 3.6.
 * IMPORTANT: While this element is still permitted in configurations in Mule 3.6 for backwards compatibility, session and
 * producer reuse for non-XA connection factories is now done by default so this element is no longer required.  This means
 * that from Mule 3.6 the following attributes of this element will be ignored.
 * <li> sessionCacheSize: the number of caches sessions will be unlimited regardless of this value.  In order to limit sessions,
 * either reduce maximum number of JMS dispatcher threads for the connector threading profile or configure limit in provider
 * connection factory or broker instance.
 * <li> cacheProducers: producers will always be cached.
 * <li> username/password: these should be configured on the jms connector element.
 */
@Deprecated
public class CachingConnectionFactoryFactoryBean extends AbstractFactoryBean<ConnectionFactory>
{

    public static final int DEFAULT_SESSION_CACHE_SIZE = 1;

    private String name;
    private boolean cacheProducers;
    private ConnectionFactory connectionFactory;
    private int sessionCacheSize = DEFAULT_SESSION_CACHE_SIZE;
    private String username;
    private String password;

    @Override
    public Class<?> getObjectType()
    {
        return ConnectionFactory.class;
    }

    @Override
    protected ConnectionFactory createInstance() throws Exception
    {
        return connectionFactory;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isCacheProducers()
    {
        return cacheProducers;
    }

    public void setCacheProducers(boolean cacheProducers)
    {
        this.cacheProducers = cacheProducers;
    }

    public ConnectionFactory getConnectionFactory()
    {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    public int getSessionCacheSize()
    {
        return sessionCacheSize;
    }

    public void setSessionCacheSize(int sessionCacheSize)
    {
        this.sessionCacheSize = sessionCacheSize;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
